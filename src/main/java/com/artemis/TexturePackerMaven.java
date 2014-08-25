package com.artemis;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_RESOURCES;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;

/**
 * Automatically generate texture atlases. This plugin wraps
 * around libhdx's TextureWrapper. 
 */
@Mojo(name="pack", defaultPhase=GENERATE_RESOURCES)
public class TexturePackerMaven extends AbstractMojo {

	/**
	 * Root folder for class files.
	 */
	@Parameter(property="project.build.outputDirectory", readonly=true)
	private File outputDirectory;
	
	@Parameter(property="project.basedir")
	private File basedir;
	
	@Parameter(defaultValue="pack")
	private String packName;
	
	@Parameter(defaultValue="src/main/sprites")
	private String assetFolder;
	
	@Parameter
	private Map<String, String> packer;
	
	@Component
	private BuildContext context;
	
	@Component
	private org.apache.maven.settings.Settings mavenSettings;
	
	private Map<Class<?>, TypedInjector> injectors = configureInjectors();
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File root = new File(basedir, assetFolder);
		if (!(root.exists() && root.isDirectory())) {
			getLog().info("Folder not found: " + root + " - no atlases to build.");
			return;
		}
		
		injectors = configureInjectors();
		
		Settings settings = new Settings();
		injectSettings(settings);
		
		try {
			TexturePacker.process(
					settings,
					root.toString(),
					outputDirectory.toString(),
					packName);
			
			getLog().info("Spritesheet(s) written.");
		} catch (Exception e) {
			getLog().error(e);
			throw new RuntimeException(e);
		}
	}



	private void injectSettings(Settings settings) {
		Map<String, Field> fields = fields(Settings.class);
		if (packer != null) {
			for (Entry<String, String> entry : packer.entrySet()) {
				if (fields.containsKey(entry.getKey())) {
					inject(settings, fields.get(entry.getKey()), entry.getValue());
				} else {
					getLog().warn(String.format("No field matching '%s' in %s.\n",
							entry.getKey(), settings.getClass()));
				}
			}
		}
	}
	
	private void inject(Settings settings, Field field, String value) {
		TypedInjector injector = injectors.get(field.getType());
		if (injector != null) {
			field.setAccessible(true);
			try {
				injector.inject(settings, field, value);
			} catch (IllegalArgumentException e) {
				getLog().error(e);
			} catch (IllegalAccessException e) {
				getLog().error(e);
			}
		} else {
			getLog().warn(String.format("Field %s with type %s has no type injector.",
					field.getName(), field.getType()));
		}
	}

	private static Map<String, Field> fields(Class<?> c) {
		Map<String, Field> fieldMap = new HashMap<String, Field>();
		for (Field f : c.getDeclaredFields()) {
			fieldMap.put(f.getName(), f);
		}
		
		return fieldMap;
	}
	
	public static abstract class TypedInjector {
		abstract void inject(Object object, Field field, String value)
			throws IllegalArgumentException, IllegalAccessException;
	}
	
	private static Map<Class<?>, TypedInjector> configureInjectors() {
		Map<Class<?>, TypedInjector> injectors = new HashMap<Class<?>, TypedInjector>();
		injectors.put(int.class, new TypedInjector() {
			@Override
			public void inject(Object object, Field field, String value) 
				throws IllegalArgumentException, IllegalAccessException {
				
				field.setInt(object, Integer.parseInt(value));
			}
		});
		injectors.put(float.class, new TypedInjector() {
			@Override
			public void inject(Object object, Field field, String value)
				throws IllegalArgumentException, IllegalAccessException {
				
				field.setFloat(object, Float.parseFloat(value));
			}
		});
		injectors.put(String.class, new TypedInjector() {
			@Override
			public void inject(Object object, Field field, String value)
				throws IllegalArgumentException, IllegalAccessException {
				
				field.set(object, value);
			}
		});
		injectors.put(boolean.class, new TypedInjector() {
			@Override
			public void inject(Object object, Field field, String value)
				throws IllegalArgumentException, IllegalAccessException {
				
				field.setBoolean(object, Boolean.parseBoolean(value));
			}
		});
		injectors.put(TextureFilter.class, new TypedInjector() {
			@Override
			public void inject(Object object, Field field, String value)
				throws IllegalArgumentException, IllegalAccessException {
				
				field.set(object, TextureFilter.valueOf(value));
			}
		});
		injectors.put(TextureWrap.class, new TypedInjector() {
			@Override
			public void inject(Object object, Field field, String value)
					throws IllegalArgumentException, IllegalAccessException {
				
				field.set(object, TextureWrap.valueOf(value));
			}
		});
		injectors.put(Format.class, new TypedInjector() {
			@Override
			public void inject(Object object, Field field, String value)
					throws IllegalArgumentException, IllegalAccessException {
				
				field.set(object, Format.valueOf(value));
			}
		});
		injectors.put(String[].class, new TypedInjector() {
			@Override
			public void inject(Object object, Field field, String value)
					throws IllegalArgumentException, IllegalAccessException {
				
				field.set(object, value.split("[, ]+"));
			}
		});
		injectors.put(float[].class, new TypedInjector() {
			@Override
			public void inject(Object object, Field field, String value)
					throws IllegalArgumentException, IllegalAccessException {
				
				String[] values = value.split("[, ]+");
				float[] floats = new float[values.length];
				for (int i = 0; values.length > i; i++) {
					floats[i] = Float.parseFloat(values[i]);
				}
				field.set(object, floats);
			}
		});
		
		return injectors;
	}
}
