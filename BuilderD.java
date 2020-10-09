package building;

// Changes to this file immediately affect the next runtime.  Treat it as a script.

import java.nio.file.Path;


/** Default implementation of a software builder.
  *
  *     @param <T> The class that defines the build targets of the project.
  */
public class BuilderD<T extends Enum<T>> implements Builder {


    /** @see BuilderBuilder#projectPackage()
      * @see BuilderBuilder#projectPath()
      */
    public BuilderD( final String projectPackage, final Path projectPath, final Class<T> targetClass ) {
        if( projectPath.isAbsolute() ) throw new IllegalArgumentException();
        this.projectPackage = projectPackage;
        this.projectPath = projectPath;
        this.targetClass = targetClass; }



   // ━━━  B u i l d e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override void build( final String target ) throws UserError {
        try { Enum.valueOf( targetClass, target ); }
        catch( IllegalArgumentException x ) {
            throw new UserError( "build: Undefined in `" + targetClass.getName() + "`: " + target ); }
        if( target.equals( "builder" )) return; } // Nothing to do, this builder is already built.



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final String projectPackage;



    private final Path projectPath;



    private final Class<T> targetClass; }



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.