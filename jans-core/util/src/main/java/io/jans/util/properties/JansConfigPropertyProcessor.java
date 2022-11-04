package io.jans.util.properties;

import sun.tools.java.Environment;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.*;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("io.jans.util.properties.JansConfigProperty")

public class JansConfigPropertyProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {


        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedProperties = env.getElementsAnnotatedWith(annotation);


            List<Element> properties = new ArrayList<>();
            properties.addAll(annotatedProperties);

            // Prepare contents for markdown document

            StringBuilder markdownContent = new StringBuilder();

            markdownContent.append("# Janssen Server Configuration Properties");
            markdownContent.append("</br>");
            markdownContent.append("</br>");
            markdownContent.append("| Property Name ");
            markdownContent.append("| Description ");
            markdownContent.append("| Mandatory ");
            markdownContent.append("| Default Value | ");
            markdownContent.append("</br>");
            markdownContent.append("|-----|-----|-----|-----|");
            markdownContent.append("</br>");

            for (Element jansProperty : properties)
            {
                JansConfigProperty propertyAnnotation = jansProperty.getAnnotation(JansConfigProperty.class);
                markdownContent.append("| "+jansProperty.getSimpleName()+" ");
                markdownContent.append("| "+propertyAnnotation.description()+" ");
                markdownContent.append("| "+propertyAnnotation.isMandatory()+" ");
                markdownContent.append("| "+propertyAnnotation.defaultValue()+" |");
                markdownContent.append("</br>");
            }

            // This would be replaced by code to write into a markdown file
            System.out.println(markdownContent.toString());
        }
        return false;
    }
}
