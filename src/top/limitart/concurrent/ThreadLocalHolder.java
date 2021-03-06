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
package top.limitart.concurrent;


import top.limitart.base.*;

/**
 * 线程变量
 *
 * @author hank
 * @version 2018/2/5 0005 21:16
 * @see ThreadLocal
 */
@ThreadSafe
public class ThreadLocalHolder<T> {
    private final ThreadLocal<T> ref = new ThreadLocal<>();

    public static <T> ThreadLocalHolder<T> create() {
        return new ThreadLocalHolder<>();
    }

    private ThreadLocalHolder() {
    }

    public @Nullable
    T get() {
        return ref.get();
    }

    public @NotNull
    T getWithInitialize(Func<T> init) {
        T t = get();
        if (t == null) {
            t = Conditions.notNull(init.run());
            set(t);
        }
        return t;
    }

    public ThreadLocalHolder set(@Nullable T t) {
        if (t == null) {
            ref.remove();
        } else {
            ref.set(t);
        }
        return this;
    }
}
