package io.jans.doc.annotation;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("io.jans.doc.annotation.DocumentedJansProperty")
public class DocumentedJansPropertyProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {


        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedProperties = env.getElementsAnnotatedWith(annotation);


            List<Element> properties = new ArrayList<>();
            properties.addAll(annotatedProperties);

            // Prepare contents for markdown document

            StringBuilder propTable = new StringBuilder();

            StringBuilder propDetails = new StringBuilder();

            // prepare document header
            propTable.append("# Janssen Server Configuration Properties");
            propTable.append("\n");
            propTable.append("\n");

            // prepare table
            propTable.append("| Property Name ");
            propTable.append("| Description ");
            propTable.append("|  | ");
            propTable.append("\n");
            propTable.append("|-----|-----|-----|");
            propTable.append("\n");

            // for each property add a row in table and add details section
            for (Element jansProperty : properties)
            {
                DocumentedJansProperty propertyAnnotation = jansProperty.getAnnotation(DocumentedJansProperty.class);

                addToTable(propTable, jansProperty, propertyAnnotation);
                addToDetails(propDetails, jansProperty, propertyAnnotation);
            }

            // This would be replaced by code to write into a markdown file
            System.out.println(propTable.toString()+"\n\n");
            System.out.println(propDetails.toString()+"\n\n");
        }
        return false;
    }

    private static void addToDetails(StringBuilder propDetails, Element jansProperty, DocumentedJansProperty propertyAnnotation) {
        propDetails.append("### "+ jansProperty.getSimpleName()+"\n\n");
        propDetails.append("- Description: "+ propertyAnnotation.description()+"\n\n");
        propDetails.append("- Required: "+ propertyAnnotation.isMandatory()+"\n\n"); //TODO: change to required and yes/no
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
