/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor8;

import static org.hibernate.jpamodelgen.util.Constants.COLLECTIONS;
import static org.hibernate.jpamodelgen.util.StringUtil.isProperty;
import static org.hibernate.jpamodelgen.util.TypeUtils.getCollectionElementType;
import static org.hibernate.jpamodelgen.util.TypeUtils.toTypeString;

class ContainsAttributeTypeVisitor extends SimpleTypeVisitor8<Boolean, Element> {

	private final Context context;
	private final TypeElement type;

	ContainsAttributeTypeVisitor(TypeElement elem, Context context) {
		this.context = context;
		this.type = elem;
	}

	@Override
	public Boolean visitDeclared(DeclaredType declaredType, Element element) {
		TypeElement returnedElement = (TypeElement) context.getTypeUtils().asElement(declaredType);

		final String returnTypeName = returnedElement.getQualifiedName().toString();
		final String collection = COLLECTIONS.get(returnTypeName);
		if (collection != null) {
			final TypeMirror collectionElementType =
					getCollectionElementType( declaredType, returnTypeName, null, context );
			final Element collectionElement = context.getTypeUtils().asElement(collectionElementType);
			if ( ElementKind.TYPE_PARAMETER == collectionElement.getKind() ) {
				return false;
			}
			returnedElement = (TypeElement) collectionElement;
		}

		return type.getQualifiedName().contentEquals( returnedElement.getQualifiedName() );
	}

	@Override
	public Boolean visitExecutable(ExecutableType executable, Element element) {
		return element.getKind() == ElementKind.METHOD
			&& isProperty( element.getSimpleName().toString(), toTypeString( executable.getReturnType() ) )
			&& executable.getReturnType().accept(this, element);
	}
}
