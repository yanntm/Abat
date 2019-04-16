package fr.atsyra.abat;


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Guice;
import com.google.inject.Injector;

import fr.atsyra.abat.AbatRuntimeModule;
import fr.atsyra.abat.AbatStandaloneSetup;
import fr.atsyra.abat.abat.AttackTree;


/**
 * Utility class for serialization of gal system
 */
public class SerializationUtil  {
	
	private static boolean isStandalone = false;
	
	private static Logger getLog() { return Logger.getLogger("fr.atsyra.abat"); }
	
	public static void setStandalone(boolean isStandalone) {
		SerializationUtil.isStandalone = isStandalone;
	}
	
	private static Injector createInjector() {
		if (isStandalone) {
			AbatStandaloneSetup gs = new AbatStandaloneSetup();
			return gs.createInjectorAndDoEMFRegistration();
		} else { 
			return Guice.createInjector(new AbatRuntimeModule());
		}
	}
	
	/**
	 * This method serialize a Abat System in the file {@code filename} 
	 * @param system The root of Abat system
	 * @param filename The output filename.
	 */
	public static void systemToFile(AttackTree system, final String filename) throws IOException
	{
		long debut = System.currentTimeMillis();

		if(! filename.endsWith(".abat"))
		{
			getLog().warning("Warning: filename '" + filename + "' should end with .abat extension ");
		}

		
		// System.out.print("Serializing...");
		
		try 
		{
			
			FileOutputStream os = new FileOutputStream(filename);
			BasicAbatSerializer bser = new BasicAbatSerializer();
			bser.serialize(system, new BufferedOutputStream(os));
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		getLog().info("Time to serialize abat into " + filename + " : " + (System.currentTimeMillis() - debut) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		
	}
	
	
	/**
	 * Returns a GAL system from a filename .gal
	 */
	public static AttackTree fileToAbatSystem(String filename)
	{
		if(! filename.endsWith(".abat"))
		{
			getLog().warning("Warning: filename '" + filename + "' should end with .abat extension ");
		}
		
		Resource res = loadResources(filename); 
		AttackTree system = (AttackTree) res.getContents().get(0);
		
		return system ;
	}


	
	/**
	 * Load a GAL file and returns a Resources from this file.
	 */
	private static Resource loadResources(String filename) 
	{
		
		Injector inj = createInjector(); 
		
		XtextResourceSet resourceSet = inj.getInstance(XtextResourceSet.class); 
		resourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
		URI uri = URI.createFileURI(filename) ; 
		Resource resource = resourceSet.getResource(uri, true);
		return resource ; 
	}

}


