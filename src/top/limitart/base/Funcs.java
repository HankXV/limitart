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
package top.limitart.base;


/**
 * 函数帮助类
 *
 * @author hank
 */
public class Funcs {

    public static <R> R invoke(@Nullable Func<R> func) {
        return func == null ? null : func.run();
    }

    public static <T, R> R invoke(@Nullable Func1<T, R> func, T t) {
        return func == null ? null : func.run(t);
    }

    public static <T1, T2, R> R invoke(@Nullable Func2<T1, T2, R> func, T1 t1, T2 t2) {
        return func == null ? null : func.run(t1, t2);
    }
}
