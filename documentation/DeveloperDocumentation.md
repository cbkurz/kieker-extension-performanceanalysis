# Developer Documentation

This dokumentation provides a short introduction to the most important concepts and packages in this repository

## Java Version 11
This is the last long term support (LTS) version that still allows reflective access to any module.
Later version enforce the module system and do not allow to access classes with reflections if not explicitly enabled.
Reflections are a feature of Epsilon that is used in this project and must be allowed, therefore version 11 is fixed.

## Main class
The starting point of the project is the class ``kieker.extension.performanceanalysis.Main``.
It accepts the arguments and tries to interpret them with the CLI library [JCommander](https://jcommander.org/).
If no matching command is given, or an error occurs while parsing, `Main` will print the CLI help to the consule and terminate.
The CLI interfaces are in the package `kieker.extension.performanceanalysis.cli`.
Each of the four transformations has its own CLI-class.

The program provides four transformations.
The Kieker2Uml transformation is written in Java, the other three are [Epsilon](https://eclipse.dev/epsilon/doc/) Scripts.

## Packages
### kieker.extension.performanceanalysis.cli
contains the classes required for the CLI.
See [JCommander](https://jcommander.org/) for documentation.

### kieker.extension.performanceanalysis.epsilon
Contains the classes that execute the Epsilon scripts.
The epsilon documentation gives a basic description on how this works in an [article](https://eclipse.dev/epsilon/doc/articles/run-epsilon-from-java/).

Each script requires a Model that it executes on.
It is key to understand which type of model is required to execute the script.
In general an implementation of the interface `org.eclipse.epsilon.eol.models.IModel` must be given,
but Epsilon provides several implementation of this interface through their [Model Connectivity Layer](https://eclipse.dev/epsilon/doc/emc/).
Some implementations are:
* `org.eclipse.epsilon.emc.emf.EmfModel` - A basic model type that allows to interact with EMF-Models
* `kieker.extension.performanceanalysis.epsilon.UmlModel` - Self implemented, (see next section for reasoning) provides access to UML-Models and is an extension of the `EmfModel` class.
* `org.eclipse.epsilon.emc.emf.xml.XmlModel` - A model type that has to be build according to a XML-Schema.
* `org.eclipse.epsilon.emc.plainxml.PlainXmlModel` - To access and manipulate plain XML, no schema or validation is applied.

Different models require different parameters.
As an abstraction from this and to save the tedious work looking up the parameters, 
the `kieker.extension.performanceanalysis.epsilon.EpsilonModelBuilder` is implemented, 
but is not required to be used.

#### UMLModel
To access UML-Models the class `kieker.extension.performanceanalysis.epsilon.UmlModel` is implemented.
This is inspired by `org.eclipse.epsilon.emc.uml.UmlModel`, however the `org.eclipse.epsilon.emc.uml` dependency requires also the `org.eclipse.uml2.uml` dependency to be present.
The `org.eclipse.uml2.uml` dependency is notoriously hard to load with the correct version and required version dependencies.
Therefore, it was downloaded by hand and provided in the **libs** folder.
This manual loading hinders gradle from recognizing the library as a provided dependency, and, as a consequence, the `UmlModel` class was implemented by hand.

### kieker.extension.performanceanalysis.kieker2uml
Contains the required classes of the Kieker2Uml transformation.
This is the only transformation that is fully implemented in Java.
It leverages a TeeTime configuration that created Kiekers MessageTraces.
A `kieker.model.system.model.MessageTrace` is a class that holds the sequence of messages that kieker has recorded.
With this sequence a UML-Model is created.
`kieker.extension.performanceanalysis.kieker2uml.teetime.SequenceDiagrammFilter` orchestrates the creation.
The following views are important for this implementation:
* Interactions - The interactions are contained within Use Cases. Each message in the `MessageTrace` represents a Message in the Interaction (also called a Sequence Diagram) 
* Components - These represent the different classes of the application. They are represented by the Lifelines in the Interactions.
The Components are the equivalent of `kieker.model.system.model.AssemblyComponent`
* Deployments - The deployment view of the application, it creates nodes and artifacts. The artifacts are a manifestation of a component.
The nodes are the equivalent of an `kieker.model.system.model.ExecutionContainer` 
and the artifacts are equivalent of `kieker.model.system.model.AllocationComponent`
* Classes - This view is *optional* and creates the classes that are present in the `MessageTraces` and their connections according to messages provided.

The MARTE Stereotypes are not provided via the profile but by a workaround with EAnnotations.
EAnnotations are a modelling concept provided by Ecore, 
they have a name (which is mapped to the name of the Stereotype) and hold Details.
Details are effectively a map of key-value-pairs.
The Details hold the attributes that would be provided by the Stereotype.
The class `kieker.extension.performanceanalysis.kieker2uml.uml.MarteSupport` 
is the implementation for this workaround and provides the appropriate methods to support the required Stereotypes.


### kieker.extension.performanceanalysis.uml2lqn uml2plantuml and uml2uml
All rely on Epsilon and the resources.
They load models appropriate to their scripts and execute them.













