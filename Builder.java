package building.Makeshift;

// Changes to this file immediately affect the next build.  Treat it as a build script.

import java.nio.file.Files;
import java.nio.file.Path;

import static building.Makeshift.Bootstrap.Unhandled;
import static building.Makeshift.Bootstrap.UserError;
import static java.nio.file.Files.isRegularFile;


/** A builder of a project’s software.  It compiles the code of the project and prepares it for use.
  * In place of the {@linkplain BuilderDefault default}, a project may define its own builder
  * by putting into its {@linkplain BuilderBuilder#internalBuildingCode(Path) building code}
  * a source file named `Builder.java`.  The class definition must be public and must include
  * a public constructor that takes no parameters.  It must inherit from the present interface.
  * It must depend on no code outside of the:<ul>
  *
  *     <li>Standard libraries</li>
  *     <li>{@linkplain BuilderBuilder#externalBuildingCode() External building code}</li>
  *     <li>{@linkplain BuilderBuilder#internalBuildingCode(Path) Internal building code}</li></ul>
  */
public interface Builder {


    /** Builds the code to the level of a given target.
      *
      *     @param targ The name of the target, or a unique substring of it.
      *     @throws UserError If `targ` does not match exactly one build target of the owning project.
      *       This exception may be thrown for other reasons as well.
      */
    public void build( String targ ) throws UserError;



    /** Gives the proper path of a builder’s source file.  The given path is either
      * `<i>{@linkplain BuilderBuilder#internalBuildingCode(Path) internalBuildingCode}</i>/Builder.java`
      * if a file exists there, or the path to the {@linkplain BuilderDefault default implementation}.
      *
      *     @param projectPath The proper path of the owning project.
      */
    public static Path implementationFile( final Path projectPath ) { // Cf. @ `BuilderBuilder`.
        Bootstrap.verify( projectPath );
        Path p = BuilderBuilder.internalBuildingCode(projectPath).resolve( "Builder.java" );
        if( !isRegularFile( p )) p = implementationFileDefault;
        return p; }



    /** The proper path of the source file for the {@linkplain BuilderDefault default implementation}.
      */
    public static final Path implementationFileDefault =
      Bootstrap.projectPath.resolve( "BuilderDefault.java" );



    /** Finds the target name that uniquely matches `targ`.  The following search conditions apply.<ul>
      *
      *     <li>Letter case is insignificant.</li>
      *     <li>Any dash character ‘-’ of `targ` is treated as an underscore ‘_’.</li>
      *     <li>Abbreviation is allowed: `targ` may be any substring of the target name
      *         that appears in no other target name.</li></ul>
      *
      *     @param targ The search term.
      *     @param targetClass The class of build targets.
      *     @throws UserError If `targ` does not match exactly one build target of `targetClass`.
      */
    public static <T extends Enum<T>> String matchingTargetName(
          final String targ, final Class<T> targetClass ) throws UserError {
        final Enum<?>[] targets;
        try { targets = (Enum[])targetClass.getMethod("values").invoke( null/*static*/ ); }
        catch( ReflectiveOperationException x ) { throw new Unhandled( x ); }
        final String nameSought = targ.toLowerCase().replace( '-', '_' ); // As per `bin/build.brec`.
        String nameFound = null;
        for( final Enum<?> t: targets ) {
            final String tS = t.toString();
            if( tS.toLowerCase().contains( nameSought )) { // Concordant with `bin/build.brec`.
                if( nameFound != null ) {
                    throw new UserError( "Ambiguous in `" + targetClass.getName() + "`: " + targ ); }
                nameFound = tS; }}
        if( nameFound == null ) {
            throw new UserError( "Unmatched in `" + targetClass.getName() + "`: " + targ ); }
        return nameFound; }}



                                                  // Copyright © 2020, 2022  Michael Allan.  Licence MIT.
