PF4J - Update
=====================
[![Travis CI Build Status](https://travis-ci.org/pf4j/pf4j-update.png)](https://travis-ci.org/pf4j/pf4j-update)
[![Maven Central](http://img.shields.io/maven-central/v/org.pf4j/pf4j-update.svg)](http://search.maven.org/#search|ga|1|pf4j-update)

The goal of this project is to supply an update mechanism for [PF4J](https://github.com/pf4j/pf4j).
It's an open source (Apache license) lightweight (around 15KB) extension for PF4J, with minimal dependencies (only pf4j and gson).

Components
-------------------
- **UpdateManager** allows you to inspect the repositories (local, remote) for updates, available (new) plugins. Also this class allows you to install available plugins, update or uninstall existing plugins.
- **UpdateRepository** defines a local/remote repository (pluggable)
- **PluginInfo** defines the plugin information for each repository's plugin
- **PluginRelease** defines a plugin release
- **FileDownloader** defines a pluggable way of downloading from a repository
- **FileVerifier** defines a pluggable way of verifying downloaded files, e.g. checksum verification

Using Maven
-------------------
In your pom.xml you must define the dependencies to PF4J artifacts with:

```xml
<dependency>
    <groupId>org.pf4j</groupId>
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

    // >> keep system up-to-date <<
    Version systemVersion = Version.valueOf("1.2.3");
    pluginManager.setSystemVersion(systemVersion);
    pluginManager.loadPlugins();

    // create update manager
    UpdateManager updateManager = new UpdateManager(pluginManager);

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

UpdateManager can work with multiple repositories (local and remote).
All repositories are either defined in a `repositories.json` file or
provided in UpdateManager's constructor.

Below I defined two repository: localhost and folder.
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

Each repository has a unique id and a URL.

In the root of this project you have a `repositories.json` file used by
the test applications.

For more information please see the test sources (UpdateTest, ...).
It's a good idea to run these tests and to see the results.

Customization
-------------
The project is made for customization and extension to your own needs. Here are some
examples:

### Tailor repository loading
First you can supply to `UpdateManager` your custom location and name
of `repositories.json` if you want it to live somewhere else.

If you need even more control, `UpdateManager` accepts repositories in
constructor and through setters.
Implement your own `UpdateRepository`, `FileDownloader` and `FileVerifier`s
to handle your own custom repsitory structures, authentication, checksum
verifications etc.

### Subclass UpdateManager
For full control, subclass `UpdateManager` and override relevant methods.

Repository structure
-------------------
Each repository exposes multiple plugins using a `plugins.json` file.  
Below I registered two plugins: _welcome-plugin_ and _hello-plugin_.

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

### plugins.json
**Fields per plugin**

|Property    |Format       |Description                        |
|------------|-------------|-----------------------------------|
|id          |string       |Unique id, mandatory               |
|name        |string       |Display name (short)               |
|description |string       |Describe your plugin               |
|provider    |string       |Name of plugin provider            |
|releases    |List         |List of releases (minimum one)     |

**Fields per release**

|Property    |Format       |Description                        |
|------------|-------------|-----------------------------------|
|version     |X.Y.Z        |Version of release ([SemVer](http://semver.org/) format) |
|date        |date         |Release date, ISO8601 or `yyyy-MM-dd` format |
|requires    |Expression   |[SemVer expression](https://github.com/zafarkhaja/jsemver#semver-expressions-api-ranges), e.g. ">=2.0.0"  |
|url         |URL-string   |Link to zip, either absolute or relative URL |
|sha512sum   |&lt;sha512-digest&gt;<br/>*or* &lt;hash-file URL&gt;<br/>*or* ".sha512" |String with SHA-512 HEX digest of file content<br/>URL to file with SHA-512 string<br/>Fetch SHA-512 file next to plugin, with `.sha512` file suffix  |


*New properties may appear in the future.*

The last (current) release of the plugin is calculated taking into account
by the _version_ property. In our example, the last release for each
plugin is the release with version _0.9.0_.

We encourage using `yyyy-MM-dd` format for release date. Localized US format
as in the examples above will also work. If the date is not parsable, it
will be set to epoch (1970-01-01) and print a warning in logs.

**NOTE**: The `requires` property was a simple X.Y.Z string in versions
up to 0.3.0, interpreted as `>=X.Y.Z`. You may want to update your old
`plugins.json` files to the new syntax.

### Example for 'hello-plugin' (plugin2):
URL from `repositories.json`: `http://localhost:8081/`
Relative URL in `plugins.json`: `pf4j-demo-plugin2/0.8.0/pf4j-demo-plugin2-0.8.0.zip`
Resulting `UpdateRepository.PluginRelease.url`: `http://localhost:8081/pf4j-demo-plugin2/0.8.0/pf4j-demo-plugin2-0.8.0.zip`

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
Much of the conversation between developers and users is managed through [mailing list](http://groups.google.com/group/pf4j).

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
