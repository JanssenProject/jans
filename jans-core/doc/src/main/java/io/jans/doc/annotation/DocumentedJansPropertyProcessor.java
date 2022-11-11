package io.jans.doc.annotation;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("io.jans.doc.annotation.DocumentedJansProperty")
public class DocumentedJansPropertyProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {


        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedProperties = env.getElementsAnnotatedWith(annotation);

            // sort alphabetically
            List<? extends Element> sortedProperties = annotatedProperties.stream()
                    .sorted((prop1, prop2)->prop1.getSimpleName().toString().toLowerCase().compareTo(prop2.getSimpleName().toString().toLowerCase()))
                    .collect(Collectors.toList());

            StringBuilder propTable = new StringBuilder();
            StringBuilder propDetails = new StringBuilder();

            // prepare document header
            propTable.append("# Janssen Server Configuration Properties");
            propTable.append("\n");
            propTable.append("\n");

            // prepare table header
            propTable.append("| Property Name ");
            propTable.append("| Description ");
            propTable.append("|  | ");
            propTable.append("\n");
            propTable.append("|-----|-----|-----|");
            propTable.append("\n");

            // for each property add a row in table and add content for the details section
            for (Element jansProperty : sortedProperties)
            {
//                System.out.println(jansProperty.getEnclosingElement().toString());
                DocumentedJansProperty propertyAnnotation = jansProperty.getAnnotation(DocumentedJansProperty.class);
                addToTable(propTable, jansProperty, propertyAnnotation);
                addToDetails(propDetails, jansProperty, propertyAnnotation);

            }
            propTable.append("\n\n");
//            String annotatedClassName = ((TypeElement)annotation.getEnclosingElement()).getQualifiedName().toString();

            createAndWriteDoc(propTable.append(propDetails.toString()), "");

            // This would be replaced by code to write into a markdown file
            System.out.println(propTable.toString()+"\n\n");
            System.out.println(propDetails.toString()+"\n\n");
        }
        return false;
    }

    private void createAndWriteDoc(StringBuilder docContent, String className) {
//        String userDirectory = System.getProperty("user.dir");
//        System.out.println("dir ->"+userDirectory);


        try{
            System.out.println("Paths -->");
            System.out.println(StandardLocation.CLASS_OUTPUT.getName());
            System.out.println(StandardLocation.SOURCE_OUTPUT);
            System.out.println(StandardLocation.CLASS_PATH);
            System.out.println(StandardLocation.ANNOTATION_PROCESSOR_PATH);
            System.out.println("Paths --<");
//            FileObject docFile = processingEnv.getFiler().createSourceFile("out.put");
            FileObject docFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,"", "out.put");
            PrintWriter docWriter = new PrintWriter(docFile.openWriter());
            docWriter.write(docContent.toString());
            docWriter.flush();
        } catch (IOException e) {
            // log to system output at compile time and exit
            System.out.println("Failed to create file for property documentation. Exiting the process");
            e.printStackTrace();
            System.exit(1);
        }

    }

    private static void addToDetails(StringBuilder propDetails, Element jansProperty, DocumentedJansProperty propertyAnnotation) {
        propDetails.append("### "+ jansProperty.getSimpleName()+"\n\n");
        propDetails.append("- Description: "+ propertyAnnotation.description()+"\n\n");
        propDetails.append("- Required: "+ (propertyAnnotation.isRequired()==Boolean.TRUE?"Yes":"No")+"\n\n"); //TODO: change to required and yes/no
        propDetails.append("- Default value: "+ propertyAnnotation.defaultValue()+"\n\n");
        propDetails.append("\n");
    }

    private static void addToTable(StringBuilder propTable, Element jansProperty, DocumentedJansProperty propertyAnnotation) {
        propTable.append("| "+ jansProperty.getSimpleName()+" ");
        propTable.append("| "+ propertyAnnotation.description()+" ");
        propTable.append("| [Details](#"+jansProperty.getSimpleName().toString().toLowerCase()+") |");
        propTable.append("\n");
    }
}
