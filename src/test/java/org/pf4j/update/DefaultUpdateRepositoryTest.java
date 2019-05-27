
/*
 * Copyright (C) 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pf4j.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DefaultUpdateRepositoryTest {

    @Test
    public void shouldUseDefaultPluginJsonFileNameWhenDeserializedFromJson() {
        Gson gson = new GsonBuilder().create();
        String repositoryJson = "{\"id\": \"localhost\",\"url\": \"http://localhost:8081/\"}";
        DefaultUpdateRepository repository = gson.fromJson(repositoryJson, DefaultUpdateRepository.class);
        assertNotNull(repository);
        assertEquals("plugins.json", repository.getPluginsJsonFileName());
    }

}
