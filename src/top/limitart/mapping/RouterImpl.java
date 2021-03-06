/*
 * Copyright (c) 2016-present The Limitart Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.limitart.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.limitart.base.Conditions;
import top.limitart.base.Func;
import top.limitart.base.Func1;
import top.limitart.base.Proc1;
import top.limitart.reflectasm.ConstructorAccess;
import top.limitart.reflectasm.MethodAccess;
import top.limitart.util.ReflectionUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 方法路由器实现
 *
 * @author hank
 * @version 2018/10/8 0008 20:48
 */
public class RouterImpl<M, C extends RequestContext<M>> implements Router<M, C> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RouterImpl.class);
    // !!这里的asm应用经测试在JAVA8下最优
    private final Map<Class<M>, RouterImpl.Context> msgs = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> managerInstances = new ConcurrentHashMap<>();
    private final Map<Class<?>, MethodAccess> methods = new ConcurrentHashMap<>();
    private Class<M> mClass;
    private Class<C> cClass;

    /**
     * 创造一个空的消息工厂
     *
     * @return
     */
    public static <M, C extends RequestContext<M>> Router empty(Class<M> mClass, Class<C> cClass) {
        return new RouterImpl(mClass, cClass);
    }

    /**
     * 通过包扫描创建消息工厂
     *
     * @param scanPackage 包名
     * @return
     * @throws ReflectiveOperationException
     * @throws IOException
     */
    public static <M, C extends RequestContext<M>> Router create(Class<M> mClass, Class<C> cClass, String scanPackage)
            throws IOException, ReflectiveOperationException, RequestDuplicatedException {
        Conditions.notNull(scanPackage, "scanPackage");
        RouterImpl factory = new RouterImpl(mClass, cClass);
        List<Class<?>> classesByPackage = ReflectionUtil.getClassesBySuperClass(scanPackage, Object.class);
        for (Class<?> clazz : classesByPackage) {
            factory.registerMapperClass(clazz, null);
        }
        return factory;
    }

    /**
     * 通过扫描包创建消息工厂
     *
     * @param scanPackage
     * @param confirmInstance 指定manager的 实例
     * @return
     * @throws IOException
     * @throws ReflectiveOperationException
     */
    public static <M, C extends RequestContext<M>> Router create(Class<M> mClass, Class<C> cClass, String scanPackage, Func1<Class<?>, Object> confirmInstance)
            throws Exception {
        Conditions.notNull(scanPackage, "scanPackage");
        RouterImpl factory = new RouterImpl(mClass, cClass);
        List<Class<?>> classesByPackage = ReflectionUtil.getClassesBySuperClass(scanPackage, Object.class);
        for (Class<?> clazz : classesByPackage) {
            factory.registerMapperClass(clazz, confirmInstance);
        }
        return factory;
    }

    public RouterImpl(Class<M> mClass, Class<C> cClass) {
        this.mClass = mClass;
        this.cClass = cClass;
    }

    /**
     * 注册一个manager
     *
     * @param mapperClass
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @Override
    public Router<M, C> registerMapperClass(Class<?> mapperClass) throws IllegalAccessException, InstantiationException, RequestDuplicatedException {
        return registerMapperClass(mapperClass, null);
    }

    /**
     * 注册一个manager
     *
     * @param mapperClass     类
     * @param confirmInstance 指定manager的 实例
     */
    public Router<M, C> registerMapperClass(Class<?> mapperClass, Func1<Class<?>, Object> confirmInstance)
            throws IllegalAccessException, InstantiationException, RequestDuplicatedException {
        MapperClass manager = mapperClass.getAnnotation(MapperClass.class);
        if (manager == null) {
            return this;
        }
        // 扫描方法
        MethodAccess methodAccess = MethodAccess.get(mapperClass);
        for (int i = 0; i < methodAccess.getMethods().size(); ++i) {
            Method method = methodAccess.getMethods().get(i);
            Mapper handler = method.getAnnotation(Mapper.class);
            if (handler == null) {
                continue;
            }
            if (!Modifier.isPublic(method.getModifiers())) {
                throw new IllegalAccessError("method must be PUBLIC:" + mapperClass.getName() + "."
                        + ReflectionUtil.getMethodOverloadName(method));
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                throw new IllegalArgumentException(mapperClass.getName() + "." + ReflectionUtil.getMethodOverloadName(method)
                        + " params length must be only ONE");
            }
            Class<?> messageType = handler.value();
            if (!mClass.isAssignableFrom(messageType)) {
                LOGGER.info("{} IS NOT a {},IGNORE.", messageType.getName(), mClass.getName());
                continue;
            }
            Class<?> paramType = parameterTypes[0];
            if (!cClass.isAssignableFrom(paramType)) {
                throw new IllegalAccessException(mapperClass.getName() + "." + ReflectionUtil.getMethodOverloadName(method)
                        + " param can only be ASSIGNABLE from " + cClass.getName());
            }
            ConstructorAccess<M> constructorAccess = (ConstructorAccess<M>) ConstructorAccess.get(messageType);
            if (msgs.containsKey(messageType)) {
                throw new RequestDuplicatedException(messageType.getName());
            }
            RouterImpl.Context messageContext = new RouterImpl.Context();
            messageContext.conAccess = constructorAccess;
            messageContext.managerClazz = mapperClass;
            messageContext.methodIndex = i;
            msgs.put((Class<M>) messageType, messageContext);
            if (!managerInstances.containsKey(mapperClass)) {
                if (confirmInstance != null) {
                    managerInstances.put(mapperClass, confirmInstance.run(mapperClass));
                } else {
                    managerInstances.put(mapperClass, mapperClass.newInstance());
                }
            }
            if (!methods.containsKey(mapperClass)) {
                methods.put(mapperClass, methodAccess);
            }
            LOGGER.info("register request " + messageType.getName() + " at " + mapperClass.getName());
        }
        return this;
    }


    /**
     * 替换掉manager的实例
     *
     * @param request
     * @param newInstance
     */
    public void replaceInstance(Class<?> mapperClass, M request, Object newInstance) {
        Conditions.notNull(mapperClass, "mapperClass");
        Conditions.notNull(request, "request");
        Conditions.notNull(newInstance, "newInstance");
        if (managerInstances.containsKey(mapperClass)) {
            managerInstances.put(mapperClass, newInstance);
        }
    }

    @Override
    public void foreachRequestClass(Consumer<Class<M>> consumer) {
        msgs.keySet().forEach(consumer);
    }

    /**
     * 根据ID获取一个消息实例
     *
     * @param requestClass
     * @return
     */
    @Override
    public M requestInstance(Class<M> requestClass) {
        if (!msgs.containsKey(requestClass)) {
            return null;
        }
        RouterImpl.Context messageContext = msgs.get(requestClass);
        return (M) messageContext.conAccess.newInstance();
    }


    @Override
    public void request(M request, Func<C> contextInstance, Proc1<MethodInvoker> proc) {
        Conditions.notNull(request, "request");
        RouterImpl.Context messageContext = msgs.get(request.getClass());
        if (messageContext == null) {
            LOGGER.error("request empty,id:" + request.getClass().getName());
            // 消息上下文不存在
            return;
        }
        C param = contextInstance.run();
        MethodAccess methodAccess = methods.get(messageContext.managerClazz);
        Object object = managerInstances.get(messageContext.managerClazz);
        proc.run(new Invoker(methodAccess, object, messageContext.methodIndex, param));
    }


    private class Context {
        private ConstructorAccess<M> conAccess;
        private Class<?> managerClazz;
        private int methodIndex;
    }

    public class Invoker implements MethodInvoker {
        private MethodAccess methodAccess;
        private Object object;
        private int methodIndex;
        private C param;

        public Invoker(MethodAccess methodAccess, Object object, int methodIndex, C param) {
            this.methodAccess = methodAccess;
            this.object = object;
            this.methodIndex = methodIndex;
            this.param = param;
        }

        @Override
        public void invoke() {
            methodAccess.invoke(object, methodIndex, param);
        }
    }
}
