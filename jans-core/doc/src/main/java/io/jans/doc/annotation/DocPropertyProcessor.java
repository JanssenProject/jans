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

@SupportedAnnotationTypes("io.jans.doc.annotation.DocProperty")
@SupportedOptions({"module"})
public class DocPropertyProcessor extends AbstractProcessor {

    String moduleName;

    // This method would be called once per class containing annotated elements
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        moduleName = processingEnv.getOptions().get("module");

        // Loop iterates once per supported annotation type by this processor
        for (TypeElement annotation : annotations) {

            // Get all the elements that are annotated by a particular annotation located across classes in this module
            Set<? extends Element> annotatedProperties = env.getElementsAnnotatedWith(annotation);

            // sort alphabetically
            List<? extends Element> sortedProperties = annotatedProperties.stream()
                    .sorted((prop1, prop2)->prop1.getSimpleName().toString().toLowerCase().compareTo(prop2.getSimpleName().toString().toLowerCase()))
                    .collect(Collectors.toList());

            StringBuilder docContents = new StringBuilder();
            StringBuilder tableContents = new StringBuilder();
            StringBuilder detailsContent = new StringBuilder();

            // prepare document header
            prepareDocTagsAndTableHeader(docContents, tableContents);

            // for each property add a row in table and add content for the details section
            for (Element jansProperty : sortedProperties)
            {
                DocProperty propertyAnnotation = jansProperty.getAnnotation(DocProperty.class);
                addToTable(tableContents, jansProperty, propertyAnnotation);
                addToDetails(detailsContent, jansProperty, propertyAnnotation);
            }
            tableContents.append("\n\n");
            docContents.append((tableContents.append(detailsContent.toString())));
            createAndWriteDoc(docContents);

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
                .append("- properties\n")
                .append("---\n")
                .append("\n")
                .append("# "+moduleName+" Configuration Properties") // add doc headers
                .append("\n")
                .append("\n");

        tableContents.append("| Property Name ") // prepare table header
                    .append("| Description ")
                    .append("|  | ")
                    .append("\n")
                    .append("|-----|-----|-----|")
                    .append("\n");
    }

    private void createAndWriteDoc(StringBuilder docContent) {

        FileObject docFile = null;
        try{
            docFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", moduleName.toLowerCase().replaceAll("\\s", "")+"-properties.md");
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

    private static void addToDetails(StringBuilder propDetails, Element jansProperty, DocProperty propertyAnnotation) {
        propDetails.append("### "+ jansProperty.getSimpleName()+"\n\n");
        propDetails.append("- Description: "+ propertyAnnotation.description()+"\n\n");
        propDetails.append("- Required: "+ (propertyAnnotation.isRequired()?"Yes":"No")+"\n\n");
        propDetails.append("- Default value: "+ propertyAnnotation.defaultValue()+"\n\n");
        propDetails.append("\n");
    }

    private static void addToTable(StringBuilder propTable, Element jansProperty, DocProperty propertyAnnotation) {
        propTable.append("| "+ jansProperty.getSimpleName()+" ");
        propTable.append("| "+ propertyAnnotation.description()+" ");
        propTable.append("| [Details](#"+jansProperty.getSimpleName().toString().toLowerCase()+") |");
        propTable.append("\n");
    }
}
