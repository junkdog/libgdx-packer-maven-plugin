A simple maven wrapper around libgdx's TexturePacker tool

### Usage

```xml
    <plugin>
        <groupId>com.github.junkdog</groupId>
        <artifactId>libgdx-packer-maven-plugin</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <executions>
            <execution>
                <goals>
                    <goal>pack</goal>
                </goals>
                <configuration>
                    <!-- Where to look for raw sprites, default value given below -->
                    <assetFolder>src/main/sprites</assetFolder>
                    <!-- Name of pack file, default value given below -->
                    <packName>pack</packName>

					<!-- The section below maps directly against
					     com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings -->
                    <packer>
                        <jpegQuality>0.92</jpegQuality>
                        <fast>true</fast>
                        <format>RGBA4444</format>
                        <wrapX>MirroredRepeat</wrapX>
                        <scale>1.0, 2.2</scale>
                        <filterMin>MipMap</filterMin>
                    </packer>
                </configuration>
            </execution>
        </executions>
    </plugin>

```
