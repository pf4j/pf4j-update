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
