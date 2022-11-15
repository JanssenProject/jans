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

@SupportedAnnotationTypes("io.jans.doc.annotation.DocFeatureFlag")
public class DocFeatureFlagProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {


        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = env.getElementsAnnotatedWith(annotation);

            // sort alphabetically
            List<? extends Element> sortedElements = annotatedElements.stream()
                    .sorted((prop1, prop2)->prop1.getSimpleName().toString().toLowerCase().compareTo(prop2.getSimpleName().toString().toLowerCase()))
                    .collect(Collectors.toList());

            StringBuilder docContents = new StringBuilder();
            StringBuilder tableContents = new StringBuilder();
            StringBuilder detailsContent = new StringBuilder();

            // prepare document header
            docContents.append("# Janssen Server Feature Flags");
            docContents.append("\n");
            docContents.append("\n");

            // prepare table header
            tableContents.append("| Feature Flag Name ");
            tableContents.append("| Description ");
            tableContents.append("|  | ");
            tableContents.append("\n");
            tableContents.append("|-----|-----|-----|");
            tableContents.append("\n");

            // for each feature flag add a row in table and add content for the details section
            for (Element element : sortedElements)
            {
                DocFeatureFlag elementAnnotation = element.getAnnotation(DocFeatureFlag.class);
                addToTable(tableContents, element, elementAnnotation);
                addToDetails(detailsContent, element, elementAnnotation);
            }
            tableContents.append("\n\n");
            createAndWriteDoc(docContents.append((tableContents.append(detailsContent.toString()))), "");

        }
        return false;
    }

    private void createAndWriteDoc(StringBuilder docContent, String className) {

        PrintWriter docWriter = null;
        try {
            FileObject docFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "module-feature-flags.md");
            docWriter = new PrintWriter(docFile.openWriter());
            docWriter.write(docContent.toString());
            docWriter.flush();
        } catch (IOException e) {
            // log to system output at compile time and exit
            System.out.println("Failed to create file for feature flag documentation. Exiting the process");
            e.printStackTrace();
            System.exit(1);
        } finally {
            docWriter.close();
        }

    }

    private static void addToDetails(StringBuilder propDetails, Element jansElement, DocFeatureFlag featureFlagAnnotation) {
        propDetails.append("### "+ jansElement.getSimpleName()+"\n\n");
        propDetails.append("- Description: "+ featureFlagAnnotation.description()+"\n\n");
        propDetails.append("- Required: "+ (featureFlagAnnotation.isRequired()==Boolean.TRUE?"Yes":"No")+"\n\n");
        propDetails.append("- Default value: "+ featureFlagAnnotation.defaultValue()+"\n\n");
        propDetails.append("\n");
    }

    private static void addToTable(StringBuilder propTable, Element jansElement, DocFeatureFlag featureFlagAnnotation) {
        propTable.append("| "+ jansElement.getSimpleName()+" ");
        propTable.append("| "+ featureFlagAnnotation.description()+" ");
        propTable.append("| [Details](#"+jansElement.getSimpleName().toString().toLowerCase()+") |");
        propTable.append("\n");
    }
}
