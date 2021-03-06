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
 * 三个
 *
 * @author hank
 */
public interface Triple<A, B, C> {
    static <A, B, C> Triple<A, B, C> ofImmutable(A a, B b, C c) {
        return new ImmutableTriple<>(a, b, c);
    }

    A getA();

    B getB();

    C getC();
}
