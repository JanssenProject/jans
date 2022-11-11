package io.jans.doc.annotation;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
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
                DocumentedJansProperty propertyAnnotation = jansProperty.getAnnotation(DocumentedJansProperty.class);
                addToTable(propTable, jansProperty, propertyAnnotation);
                addToDetails(propDetails, jansProperty, propertyAnnotation);
            }
            propTable.append("\n\n");
            createAndWriteDoc(propTable.append(propDetails.toString()), "");

        }
        return false;
    }

    private void createAndWriteDoc(StringBuilder docContent, String className) {

        PrintWriter docWriter = null;
        try {
            FileObject docFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "out.put");
            docWriter = new PrintWriter(docFile.openWriter());
            docWriter.write(docContent.toString());
            docWriter.flush();
        } catch (IOException e) {
            // log to system output at compile time and exit
            System.out.println("Failed to create file for property documentation. Exiting the process");
            e.printStackTrace();
            System.exit(1);
        } finally {
            docWriter.close();
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
