# keytocomment-kotlin

The simple [Beatunes](https://www.beatunes.com) "keytocomment" 
[example plugin](https://github.com/beatunes/plugin-samples), converted to [Kotlin](https://kotlinlang.org).

This also uses [Gradle](https://gradle.org) to build the plugin, because I don't have much experience with
[Maven](https://maven.apache.org). This required a trivial change to `plugin.xml` (see the
[keytocomment-gradle](https://github.com/jlmelville/keytocomment-gradle) repo for more details).

I did this purely because I wanted to write a Beatunes plugin, but thought I might as well try and teach myself a bit of
non-trivial Kotlin at the same time. The initial release is the result of pasting the Java code into IntelliJ and 
letting it convert it into Kotlin automatically. It may be useful as a gentle introduction to the differences between 
Kotlin and Java, but not provide any great improvements in clarity or brevity or what not. Later releases may make more 
changes to take advantage of Kotlin features and idioms.

## Building

Windows:
```Batchfile
gradlew.bat build
```

Linux:
```Shell
./gradlew build
```

You can find the built JAR file as `build/libs/keytocomment-gradle-<version>.jar`.

## Installing

### Creating a Kotlin Runtime Jar

You will need the Kotlin runtime and its dependencies. I'm sure there's an incredibly straightforward way to
download this that I am missing, but I ended up using gradle to generate a fat jar manually 
([this tutorial](http://www.mkyong.com/gradle/gradle-create-a-jar-file-with-dependencies/) providing the key 
jar-creating information). In an empty directory save the following as `build.gradle`:

```Gradle
apply plugin: 'kotlin'

ext.kotlin_version = '1.1.51'
version kotlin_version

repositories {
    mavenCentral()
}

dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlin_version"
}

task fatJar(type: Jar) {
    baseName = 'kotlin-fat'
    from { configurations.runtime.collect { it.isDirectory() ? it : zipTree(it) } }
}

buildscript {
    ext.kotlin_version = '1.1.51'

    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}
```

Then run (assuming gradle is on your `PATH`, otherwise by using your favored way of invoking gradle):

```Shell
gradle fatJar
```

You should then find `kotlin-fat-1.1.51.jar` in the `build/libs` subdirectory.

Put that in the `lib` directory of your Beatunes installation. On my Windows 10 machine, it can be found at 
`C:\Program Files\beaTunes5\lib`.

At least you only need to do this once.

### Deploying the plugin

Copy the plugin jar file into your `plugins` directory. On my Windows 10 installation, it's in the user's 
`AppData\Local\tagtraum industries\beaTunes\plugins` directory, rather than where Beatunes itself is installed.

## Test it works

1. Under Edit > Preferences > Plugins, on the Installed tab should be a plugin called 'Copy key to comment'. If not, it
didn't install correctly.
1. Select some songs from your library, ideally ones where the comments don't already contain the key information and 
you don't care that we're going to overwrite them.
1. Right click (or whatever you do to bring up the context menu) and choose 'Analyze'.
1. In the Analyze Options that appears, go to the 'Analyze key' section and make sure that check box is checked. 
But make sure the 'Copy key to comment using' check box is *not* checked. This isn't the plugin, it's built-in 
functionality.
1. Scroll further down, probably to the bottom, where there should be a 'Copy key to comment'. *This* is our plugin. 
Check the box. Choose whatever key format you like.
1. Click 'Analyze' and let 'er rip.
1. Once you're done, the 'Comments' field should contain the key information, which, assuming you left the format as 
'Traditional (OK)', will look something like 'G major (2d)'.

## License

The Kotlin source, like the original Java example code, is 
[LGPL 2.1](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html).

## See Also

If you don't care about Kotlin, but would like to build Beatunes plugins with Gradle, see 
[keytocomment-gradle](https://github.com/jlmelville/keytocomment-gradle).