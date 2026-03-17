/*
 * Copyright (c) 2024. MangoRage
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.mangorage.tsml.gradle.core.resolvers;

public record ResolveDependency(String groupID, String nameID, String versionID, String nameID2, String versionID2) {
    public static ResolveDependency of(String groupID, String nameID, String versionID) {
        return new ResolveDependency(groupID, nameID, versionID, nameID, versionID);
    }

    public CompletedResolver complete(String groupID, String nameID, String versionID, String nameID2, String versionID2) {
        return new CompletedResolver(true, new ResolveDependency(groupID, nameID, versionID, nameID2, versionID2));
    }

    public CompletedResolver pass() {
        return new CompletedResolver(false, this);
    }
}
