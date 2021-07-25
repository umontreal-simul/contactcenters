# ContactCenters: Contact Center Simulation Library for Java

ContactCenters is a Java library for writing contact center
simulators.  It was developed in the Département d'Informatique et de
Recherche Opérationnelle (DIRO), at the Université de Montréal.  It
supports multi-skill and blend contact centers with complex and
arbitrary routing, dialing policies, and arrival processes.  The
programmer can alter the simulation logic in many ways, without
modifying the source code of the library.  A simulator can
interoperate with other libraries, e.g., for optimization and
statistical analysis.

For more information, please visit http://simul.iro.umontreal.ca/contactcenters/index.html 


## Compilation

Run `mvn package` to create the jar file. 
The jar file will be generated in the `target` folder.
The jar file `jar-with-dependencies` can be used as a stand-alone jar.

Run `mvn install` to install the compiled library in the local maven repository.


## Execution

The `bin` folder includes some scripts to simplify the execution of ContactCenters.
For example, the script `bin/mskcallcentersim` executes the class `umontreal.iro.lecuyer.contactcenters.msk.CallCenterSim`.


## XML Schemas

To compile the XML schemas, use the script `bin/compileSchemas`.

To generate the XML schemas documentation, run the script `bin/compileSchemasDoc`.
Note that it requires the program DocFlex.


## Documentation

Currently, the Java documentation is written with **TCode 2** doclet, which allows LaTeX commands in the comments.
However, executing standard `javadoc` will fail.

User's guide and examples are available in folder `doc`.
The user needs to compile the LaTeX files to generate the PDF files.


## Contact

Send email to: [simul@iro.umontreal.ca](mailto:simul@iro.umontreal.ca)
