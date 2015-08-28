PF4J - Update
=====================
[![Travis CI Build Status](https://travis-ci.org/decebals/pf4j-update.png)](https://travis-ci.org/decebals/pf4j-update)
[![Maven Central](http://img.shields.io/maven-central/v/ro.fortsoft.pf4j/pf4j-update.svg)](http://search.maven.org/#search|ga|1|pf4j-update)

The goal of this project is to supply an update mechanism for [PF4J](https://github.com/decebals/pf4j).  
It's an open source (Apache license) lightweight (around 15KB) extension for PF4J, with minimal dependencies (only pf4j and gson).

Components
-------------------
- **UpdateManager** allows you to inspect the repositories (local, remote) for updates, available (new) plugins. Also this class allows you to install available plugins, update or uninstall existing plugins.
- **UpdateRepository** defines a local/remote repository
- **PluginInfo** defines the plugin information for each repository's plugin
- **PluginRelease** defines a plugin release

Using Maven
-------------------
In your pom.xml you must define the dependencies to PF4J artifacts with:

```xml
<dependency>
    <groupId>ro.fortsoft.pf4j</groupId>
    <artifactId>pf4j-update</artifactId>
    <version>${pf4j-update.version}</version>
</dependency>    
```

where ${pf4j-update.version} is the last pf4j-update version.

You may want to check for the latest released version using [Maven Search](http://search.maven.org/#search%7Cga%7C1%7Cpf4j-update)

How to use
-------------------
It's very simple to add pf4j-update in your application:

```java
public static void main(String[] args) {
    ...
    
    // create plugin manager
    PluginManager pluginManager = new DefaultPluginManager();
    pluginManager.loadPlugins();
    
    // create update manager
    UpdateManager updateManager = new UpdateManager(pluginManager);

    // >> keep system up-to-date <<
    Version systemVersion = pluginManager.getSystemVersion();

    // check for updates
    if (updateManager.hasUpdates()) {
        List<UpdateRepository.PluginInfo> updates = updateManager.getUpdates();
        for (UpdateRepository.PluginInfo plugin : updates) {
            UpdateRepository.PluginRelease lastRelease = plugin.getLastRelease(systemVersion);
            String lastVersion = lastRelease.version;
            String installedVersion = pluginManager.getPlugin(plugin.id).getDescriptor().getVersion().toString();
            updateManager.updatePlugin(plugin.id, lastRelease.url);
        }
    }
    
    // check for available (new) plugins
    if (updateManager.hasAvailablePlugins()) {
        List<UpdateRepository.PluginInfo> availablePlugins = updateManager.getAvailablePlugins();
        for (UpdateRepository.PluginInfo plugin : availablePlugins) {
            UpdateRepository.PluginRelease lastRelease = plugin.getLastRelease(systemVersion);
            String lastVersion = lastRelease.version;
            updateManager.installPlugin(lastRelease.url);
        }
    }

    ...
}
```    

The library has very few components. The main component is the **UpdateManager**.  
This class contains methods for repositories inspection
```java
public List<UpdateRepository.PluginInfo> getAvailablePlugins();
public boolean hasAvailablePlugins();
public List<UpdateRepository.PluginInfo> getUpdates();
public boolean hasUpdates();
public List<UpdateRepository.PluginInfo> getPlugins();
public List<UpdateRepository> getRepositories();
```
and methods for plugin handling
```java
public boolean installPlugin(String url);
public boolean updatePlugin(String id, String url);
public boolean uninstallPlugin(String id);
```

UpdateManager can work with multiple repositories (local and remote). All repositories are defined in a `repositories.json` file.

Bellow I defined two repository: localhost and folder.
```json
[
  {
    "id": "localhost",
    "url": "http://localhost:8081/"
  },
  {
    "id": "folder",
    "url": "file:/home/decebal/work/pf4j-update/downloads/"
  }  
]
```

Each repository defined in `repositories.json` has an unique id and an url.  

In the root of the project you have a repositories.json file used by the test applications.  

For more information please see the test sources (UpdateTest, ...). It's a good idea to run these tests and to see the results.

Repository structure
-------------------
Each repository exposes multiple plugins using a `plugins.json` file.  
Bellow I registered two plugins: _welcome-plugin_ and _hello-plugin_.
```json
[
  {
    "id": "welcome-plugin",
    "description": "Welcome plugin",
    "releases": [
      {
        "version": "0.8.0",
        "date": "Jun 5, 2014 9:00:35 PM",
        "url": "pf4j-demo-plugin1/0.8.0/pf4j-demo-plugin1-0.8.0.zip"
      },    
      {
        "version": "0.9.0",
        "date": "Jun 25, 2014 9:58:35 PM",
        "url": "pf4j-demo-plugin1/0.9.0/pf4j-demo-plugin1-0.9.0.zip"
      }
    ]
  },
  {
    "id": "hello-plugin",
    "description": "Hello plugin",
    "releases": [
      {
        "version": "0.8.0",
        "date": "Jun 5, 2014 9:12:35 PM",
        "url": "pf4j-demo-plugin2/0.8.0/pf4j-demo-plugin2-0.8.0.zip"
      },
      {
        "version": "0.9.0",
        "date": "Jun 25, 2014 9:58:35 PM",
        "url": "pf4j-demo-plugin2/0.9.0/pf4j-demo-plugin2-0.9.0.zip"
      }
    ]
  }
]
```

Each plugin registered in `plugins.json` has an unique id, a description, a (display) name, the provider name,
a project url, and a list with releases. New properties may appear in the future.  
The **id** field is mandatory. Also the **releases** field must contains at least one element.  
The last (current) release of the plugin is calculated taking into account by the _version_ property. In our example,
the last release for each plugin is the release with version _0.8.0_.  
The **url** value (from plugins.json) for each release is relative to the repository's url but the value for **UpdateRepository.PluginRelease.url** is absolute.   
For plugin1 example:  
`"url": "http://localhost:8081/"` (repositories.json)  
`"url": "pf4j-demo-plugin2/0.8.0/pf4j-demo-plugin2-0.8.0.zip"` (plugins.json)  
and the `UpdateRepository.PluginRelease.url` value for release 0.8.0 of plugin1 is   `http://localhost:8081/pf4j-demo-plugin2/0.8.0/pf4j-demo-plugin2-0.8.0.zip`  
NOTE: Don't forget to put */* to the end of url values from repositories.json.  

In the _downloads_ folder of the project you have a repository (plugins.json and artifacts - plugins archives) used by the test applications.
The structure of the repository is:
- plugin1  
    - 0.8.0  
        - plugin1.zip  
    - 0.9.0  
        - plugin1.zip  
- plugin2  
    - 0.8.0  
        - plugin2.zip  
    - 0.9.0        
        - plugin2.zip  
- plugins.json   

For each plugin you have a folder (plugin1, plugin2) that contains subfolder for each version (0.8.0, 0.9.0). 
In each version folder you have the plugin archive (.zip) according to PF4J specification.  

Mailing list
--------------
Much of the conversation between developers and users is managed through [mailing list] (http://groups.google.com/group/pf4j).

Versioning
------------
PF4J will be maintained under the Semantic Versioning guidelines as much as possible.

Releases will be numbered with the follow format:

`<major>.<minor>.<patch>`

And constructed with the following guidelines:

* Breaking backward compatibility bumps the major
* New additions without breaking backward compatibility bumps the minor
* Bug fixes and misc changes bump the patch

For more information on SemVer, please visit http://semver.org/.

License
--------------
Copyright 2014 Decebal Suiu

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with
the License. You may obtain a copy of the License in the LICENSE file, or at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
