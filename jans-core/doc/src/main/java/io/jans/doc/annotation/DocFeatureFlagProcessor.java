package io.jans.doc.annotation;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("io.jans.doc.annotation.DocFeatureFlag")
@SupportedOptions({"module"})
public class DocFeatureFlagProcessor extends AbstractProcessor {

    String moduleName;
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        moduleName = processingEnv.getOptions().get("module");

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
            prepareDocTagsAndTableHeader(docContents, tableContents);

            // for each feature flag add a row in table and add content for the details section
            for (Element element : sortedElements)
            {
                DocFeatureFlag elementAnnotation = element.getAnnotation(DocFeatureFlag.class);
                addToTable(tableContents, element, elementAnnotation);
                addToDetails(detailsContent, element, elementAnnotation);
            }
            tableContents.append("\n\n");
            createAndWriteDoc(docContents.append((tableContents.append(detailsContent.toString()))));

        }
        return false;
    }

    private void prepareDocTagsAndTableHeader(StringBuilder docContents, StringBuilder tableContents) {
        // add tags
        docContents.append("---\n")
                .append("tags:\n")
                .append("- administration\n")
                .append("- reference\n")
                .append("- json\n")
                .append("- feature-flags\n")
                .append("---\n")
                .append("\n")
                .append("# "+moduleName+" Feature Flags") // add doc header
                .append("\n")
                .append("\n");

        tableContents.append("| Feature Flag Name ") // prepare table header
                .append("| Description ")
                .append("|  | ")
                .append("\n")
                .append("|-----|-----|-----|")
                .append("\n");
    }

    private void createAndWriteDoc(StringBuilder docContent) {

        FileObject docFile = null;
        try{
            docFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", moduleName.toLowerCase().replaceAll("\\s", "")+"-feature-flags.md");
        }
        catch (IOException ioe){
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, this.getClass().getName()+": Error occurred while creating annotation documentation file");
        }
        if(docFile!=null){
            try(PrintWriter docWriter = new PrintWriter(docFile.openWriter());) {
                docWriter.write(docContent.toString());
                docWriter.flush();
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, this.getClass().getName()+": Error occurred while writing annotation documentation file");
            }
        }
    }

    private static void addToDetails(StringBuilder propDetails, Element jansElement, DocFeatureFlag featureFlagAnnotation) {
        propDetails.append("### "+ jansElement.getSimpleName()+"\n\n");
        propDetails.append("- Description: "+ featureFlagAnnotation.description()+"\n\n");
        propDetails.append("- Required: "+ (featureFlagAnnotation.isRequired()?"Yes":"No")+"\n\n");
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
