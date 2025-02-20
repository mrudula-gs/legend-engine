// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.deephaven.test.shared;

import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL_Impl;

public class DeephavenCommand
{
    public static String GET_SERVER_FUNCTION = "getDeephavenTestServer_String_1__URL_1_";

    public static Root_meta_pure_functions_io_http_URL getServer() // String imageTag
    {
        Root_meta_pure_functions_io_http_URL_Impl url = new Root_meta_pure_functions_io_http_URL_Impl("deephavenUrl");
        url._host("localhost");
        url._port(10000);
        url._path("/");
        return url;
    }
}
