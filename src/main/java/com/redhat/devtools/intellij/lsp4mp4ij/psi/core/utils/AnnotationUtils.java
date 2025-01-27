/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierListOwner;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.util.Ranges;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java annotations utilities.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/AnnotationUtils.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/AnnotationUtils.java</a>
 *
 */
public class AnnotationUtils {

	public static boolean hasAnnotation(PsiElement annotatable, String annotationName) {
		return getAnnotation(annotatable, annotationName) != null;
	}


	/**
	 * Returns the annotation from the given <code>annotatable</code> element with
	 * the given name <code>annotationName</code> and null otherwise.
	 *
	 * @param annotatable    the class, field which can be annotated.
	 * @param annotationName the annotation name
	 * @return the annotation from the given <code>annotatable</code> element with
	 *         the given name <code>annotationName</code> and null otherwise.
	 */
	public static PsiAnnotation getAnnotation(PsiElement annotatable, String annotationName) {
		if (annotatable instanceof PsiAnnotationOwner) {
			return getAnnotation(annotationName, ((PsiAnnotationOwner) annotatable).getAnnotations());
		} else if (annotatable instanceof PsiModifierListOwner) {
			return getAnnotation(annotationName, ((PsiModifierListOwner) annotatable).getAnnotations());
		}
		return null;
	}

	@Nullable
	private static PsiAnnotation getAnnotation(String annotationName, PsiAnnotation[] annotations) {
		for (PsiAnnotation annotation : annotations) {
			if (isMatchAnnotation(annotation, annotationName)) {
				return annotation;
			}
		}
		return null;
	}

	/**
	 * Returns true if the given annotation match the given annotation name and
	 * false otherwise.
	 *
	 * @param annotation     the annotation.
	 * @param annotationName the annotation name.
	 * @return true if the given annotation match the given annotation name and
	 *         false otherwise.
	 */
	public static boolean isMatchAnnotation(PsiAnnotation annotation, String annotationName) {
		if(annotation == null) {
		    return false;
        }
		else if ( annotation.getQualifiedName() == null){
			return false;
		}
		else {
			return annotationName.endsWith(annotation.getQualifiedName());
		}
	}

	/**
	 * Returns the value of the given member name of the given annotation.
	 *
	 * @param annotation the annotation.
	 * @param memberName the member name.
	 * @return the value of the given member name of the given annotation.
	 */
	public static String getAnnotationMemberValue(PsiAnnotation annotation, String memberName) {
		PsiAnnotationMemberValue member = annotation.findDeclaredAttributeValue(memberName);
		String value = member != null && member.getText() != null ? member.getText() : null;
		if (value != null && value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
			value = value.substring(1, value.length() - 1);
		}
		return value;
	}

	public static PsiAnnotationMemberValue getAnnotationMemberValueExpression(PsiAnnotation annotation, String memberName) {
		return annotation.findDeclaredAttributeValue(memberName);
	}

	/**
	 * Retrieve the value and range of an annotation member given a supported list
	 * of annotation members
	 *
	 * @param annotation            the annotation of the retrieved members
	 * @param annotationSource      the qualified name of the annotation
	 * @param annotationMemberNames the supported members of the annotation
	 * @param position              the hover position
	 * @param typeRoot              the java type root
	 * @param utils                 the utility to retrieve the member range
	 *
	 * @return an AnnotationMemberInfo object if the member exists, null otherwise
	 */
	public static AnnotationMemberInfo getAnnotationMemberAt(PsiAnnotation annotation, String[] annotationMemberNames,
															 Position position, PsiFile typeRoot, IPsiUtils utils) {
		String annotationSource = annotation.getText();
		TextRange r = annotation.getTextRange();
		String annotationMemberValue = null;
		for (String annotationMemberName : annotationMemberNames) {
			annotationMemberValue = getAnnotationMemberValue(annotation, annotationMemberName);
			if (annotationMemberValue != null) {
				// A regex is used to match the member and member value to find the position
				Pattern memberPattern = Pattern.compile(".*[^\"]\\s*(" + annotationMemberName + ")\\s*=.*",
						Pattern.DOTALL);
				Matcher match = memberPattern.matcher(annotationSource);
				if (match.matches()) {
					int offset = annotationSource.indexOf(annotationMemberValue, match.end(1));
					Range range = utils.toRange(typeRoot, r.getStartOffset() + offset, annotationMemberValue.length());

					if (!position.equals(range.getEnd()) && Ranges.containsPosition(range, position)) {
						return new AnnotationMemberInfo(annotationMemberValue, range);
					}
				}
			}
		}

		return null;

	}

}
