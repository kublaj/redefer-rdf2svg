# Introduction

The RDF to SVG service **RDF2SVG** generates a **SVG** graphical representation of the input **RDF** following the workflow:

    RDF -rules-> Graph Visualization RDF Model -xslt-> .DOT File -Graphviz-> SVG

The input **RDF** goes through a set of **Jena rules** that select the parts of the **RDF** structure to show and models them also using **RDF**. Currently there are **4** set of rules that:
 
* Represent the whole **graph** structure generated by RDF **triples**.
* Represent just the **hierarchical** structure of the **classes** defined in the input RDF.
* Represent the hierarchy of **parts** in the input RDF.
* (Beta) Represent the **OWL constructs** in the input RDF defining an **ontology**.

As a result of the reasoning based on the selected set of rules, an output **RDF** modelling the graph structure to visualize is generated. This RDF is then processed by an **XSL** transformation that models the same structure using the **Graphviz** .DOT format. 

The .DOT file is then sent to **Graphviz**, which layouts the graph visualization and **renders** it as an **SVG**, the final output of the **RDF to SVG web service**.

# Installation

To build the deployment **WAR** file using the source code and **Maven**:

    mvn clean package
    
A **prebuilt WAR** file is available from: http://rhizomik.net/html/redefer/rdf2svg.war

**RDF2SVG** requires that **Graphviz** is installed in the machine it is deployed in. **Graphviz** can be downloaded 
from http://www.graphviz.org. Once installed, **configure** RDF2SVG so it can find it. In the 
**rdf2svg/WEB-INF/web.xml** file, once the WAR file is decompressed, edit the **dotPath** parameter to point
to the appropriate executable (for instance **/usr/local/bin/dot** or **C:\\Programs\\GraphViz\\bin\\dot.exe**).


# Use

When deployed in a local servlet container like Tomcat, the RDF2SVG service will be available at something like: **http://localhost:8080/rdf2svg/render**

(The service is deployed at **rhizomik.net/redefer-services/render** and it can be tested from [http://rhizomik.net/redefer/rdf2svg-form/](http://rhizomik.net/redefer/rdf2svg-form/))

It can called using **GET** or **POST**. The former is recommended when the RDF to be transformed is available from a URL, the latter when direct input is provided.

The parameters of the service are:

*   **rdf=RDF | URL**: the RDF to be processed or a URL (content negotiated) where it can be retrieved from.
*   **format=RDF/XML | N-TRIPLE | N3**: the format the input RDF is serialised in.
*   **mode=svg | snippet**: defines if the output is a full SVG file or just a snippet for inclusion in other web pages (default "svg").
*   **rules=URL**: it specifies the set of Jena rules to be used in order to determine what is going to be rendered. Available examples:

    * **http://localhost:8080/rdf2svg/rules/showgraph.jrule**: draws the whole graph in a condensed way (boxes for resources and their types, CURIEs,...)
    * **http://localhost:8080/rdf2svg/rules/showclasshierarchy.jrule**: draws just the classes subsumption hierarchy.
    * **http://localhost:8080/rdf2svg/rules/showpartshierarchy.jrule**: draws the "part of" hierarchy in the input.
    * **http://localhost:8080/rdf2svg/rules/showontology.jrule**: shows the OWL constructs in the input ontology (beta).

Examples using GET and the RDF2SVG service deployed at **rhizomik.net/redefer-services/render**:

*   **Show a RDF graph**:
    
    [http://rhizomik.net/redefer-services/render?rdf=http://rhizomik.net/html/ontologies/mpeg7ontos/examples/descriptionExample002.rdf&format=RDF/XML&mode=svg&rules=http://rhizomik.net/html/redefer/rdf2svg/showgraph.jrule]
    (http://rhizomik.net/redefer-services/render?rdf=http://rhizomik.net/html/ontologies/mpeg7ontos/examples/descriptionExample002.rdf&format=RDF/XML&mode=svg&rules=http://rhizomik.net/html/redefer/rdf2svg/showgraph.jrule)

*   **Show the class hierarchy in an ontology**:
    
    [http://rhizomik.net/redefer-services/render?rdf=https://raw.github.com/structureddynamics/Bibliographic-Ontology-BIBO/1.3/bibo.xml.owl&format=RDF/XML&mode=svg&rules=http://rhizomik.net/html/redefer/rdf2svg/showclasshierarchy.jrule]
    (http://rhizomik.net/redefer-services/render?rdf=https://raw.github.com/structureddynamics/Bibliographic-Ontology-BIBO/1.3/bibo.xml.owl&format=RDF/XML&mode=svg&rules=http://rhizomik.net/html/redefer/rdf2svg/showclasshierarchy.jrule)
    
*   **Show ontology**:
    
    [http://rhizomik.net/redefer-services/render?rdf=http://rhizomik.net/ontologies/2009/09/copyrightonto.owl&format=RDF/XML&mode=svg&rules=http://rhizomik.net/html/redefer/rdf2svg/showontology.jrule]
    (http://rhizomik.net/redefer-services/render?rdf=http://rhizomik.net/ontologies/2009/09/copyrightonto.owl&format=RDF/XML&mode=svg&rules=http://rhizomik.net/html/redefer/rdf2svg/showontology.jrule)
