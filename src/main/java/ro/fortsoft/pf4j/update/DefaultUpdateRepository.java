/*
 * Copyright 2014 Decebal Suiu
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
package ro.fortsoft.pf4j.update;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.update.PluginInfo.PluginRelease;

import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Decebal Suiu
 */
public class DefaultUpdateRepository implements UpdateRepository {

    private static final Logger log = LoggerFactory.getLogger(DefaultUpdateRepository.class);

    private String pluginsJsonFileName = "plugins.json";

    private String id;
    private URL url;
    private Map<String, PluginInfo> plugins;

    public DefaultUpdateRepository(String id, URL url) {
        this.id = id;
        this.url = url;
    }

    public DefaultUpdateRepository(String id, URL url, String pluginsJsonFileName) {
        this(id, url);
        this.pluginsJsonFileName = pluginsJsonFileName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public Map<String, PluginInfo> getPlugins() {
        if (plugins == null) {
            initPlugins();
        }

        return plugins;
    }

    @Override
    public PluginInfo getPlugin(String id) {
        return getPlugins().get(id);
    }

    private void initPlugins() {
        Reader pluginsJsonReader;
        try {
            URL pluginsUrl = new URL(getUrl(), pluginsJsonFileName);
            log.debug("Read plugins of '{}' repository from '{}'", id, pluginsUrl);
            pluginsJsonReader = new InputStreamReader(pluginsUrl.openStream());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            plugins = Collections.emptyMap();
            return;
        }

        Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new LenientDateTypeAdapter()).create();
        PluginInfo[] items = gson.fromJson(pluginsJsonReader, PluginInfo[].class);
        plugins = new HashMap<>(items.length);
        for (PluginInfo p : items) {
            for (PluginRelease r : p.releases) {
                try {
                    r.url = new URL(getUrl(), r.url).toString();
                    if (r.date.getTime() == 0) {
                        log.warn("Illegal release date when parsing {}@{}, setting to epoch", p.id, r.version);
                    }
                } catch (MalformedURLException e) {
                    log.warn("Skipping release {} of plugin {} due to failure to build valid absolute URL. Url was {}{}", r.version, p.id, getUrl(), r.url);
                }
            }
            p.setRepositoryId(getId());
            plugins.put(p.id, p);
        }
        log.debug("Found {} plugins in repository '{}'", plugins.size(), id);
    }

    /**
     * Causes plugins.json to be read again to look for new updates from repos
     */
    @Override
    public void refresh() {
        plugins = null;
    }

    @Override
    public FileDownloader getFileDownloader() {
        return new SimpleFileDownloader();
    }

    public String getPluginsJsonFileName() {
        return pluginsJsonFileName;
    }

    /**
     * Choose another file name than plugins.json
     * @param pluginsJsonFileName the name (relative) of plugins.json file
     */
    public void setPluginsJsonFileName(String pluginsJsonFileName) {
        this.pluginsJsonFileName = pluginsJsonFileName;
    }

    /* Fork of com.google.gson.internal.bind.DateTypeAdapter */
    private static class LenientDateTypeAdapter extends TypeAdapter<Date> {
        public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
          @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
          public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            return typeToken.getRawType() == Date.class ? (TypeAdapter<T>) new LenientDateTypeAdapter() : null;
          }
        };

        private final DateFormat enUsFormat
            = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US);
        private final DateFormat localFormat
            = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
        private final DateFormat iso8601Format = buildIso8601Format();
        private final DateFormat shortFormat = buildShortFormat();

        private static DateFormat buildIso8601Format() {
          DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
          iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
          return iso8601Format;
        }

        private static DateFormat buildShortFormat() {
          DateFormat shortFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
          shortFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
          return shortFormat;
        }

        @Override public Date read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          return deserializeToDate(in.nextString());
        }

        private synchronized Date deserializeToDate(String json) {
          try {
            return localFormat.parse(json);
          } catch (ParseException ignored) {
          }
          try {
            return enUsFormat.parse(json);
          } catch (ParseException ignored) {
          }
          try {
            return iso8601Format.parse(json);
          } catch (ParseException ignored) {
          }
          try {
            return shortFormat.parse(json);
          } catch (ParseException e) {
            return new Date(0);
          }
        }

        @Override public synchronized void write(JsonWriter out, Date value) throws IOException {
          if (value == null) {
            out.nullValue();
            return;
          }
          String dateFormatAsString = enUsFormat.format(value);
          out.value(dateFormatAsString);
        }
    }

}
