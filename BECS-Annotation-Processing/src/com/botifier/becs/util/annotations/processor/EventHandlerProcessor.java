package com.botifier.becs.util.annotations.processor;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import com.botifier.becs.util.annotations.EventHandler;
import com.google.auto.service.AutoService;


@SupportedAnnotationTypes(value = { "com.botifier.becs.util.annotations.EventHandler" })
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class EventHandlerProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (Element element : roundEnv.getElementsAnnotatedWith(EventHandler.class)) {
			if (element.getKind() != ElementKind.METHOD) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
								"@EventHandler can only be applied to methods",
								element);
				continue;
			}

			ExecutableElement method = (ExecutableElement) element;
			EventHandler annotation = method.getAnnotation(EventHandler.class);

			if (method.getParameters().size() != 1) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
						"@EventHandler method must only have one parameter",
						element);
				continue;
			}

			VariableElement param = method.getParameters().get(0);
			TypeMirror paramType = param.asType();

			try {
				TypeMirror eventType = processingEnv.getElementUtils()
						.getTypeElement(annotation.event().getCanonicalName())
						.asType();

				Types typeUtils = processingEnv.getTypeUtils();
				if (!typeUtils.isAssignable(eventType, paramType)) {
					processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
							String.format("Parameter type %s is not compatible with event type %s",
									paramType,
									eventType),
							param);
				}
			} catch (MirroredTypeException e) {
				TypeMirror eventType = e.getTypeMirror();
				if (!processingEnv.getTypeUtils().isAssignable(eventType, paramType)) {
					processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
							String.format("Parameter type %s is not compatible with event type %s",
									paramType,
									eventType),
							param);
				}
			}
		}
		return true;
	}

}
