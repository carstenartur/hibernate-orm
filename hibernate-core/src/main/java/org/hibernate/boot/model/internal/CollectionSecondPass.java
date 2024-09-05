/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;

import org.jboss.logging.Logger;

/**
 * Collection second pass
 *
 * @author Emmanuel Bernard
 */
public abstract class CollectionSecondPass implements SecondPass {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, CollectionSecondPass.class.getName() );

	private final Collection collection;

	public CollectionSecondPass(Collection collection) {
		this.collection = collection;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses)
			throws MappingException {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Second pass for collection: %s", collection.getRole() );
		}

		secondPass( persistentClasses );
		collection.createAllKeys();

		if ( LOG.isDebugEnabled() ) {
			String msg = "Mapped collection key: " + columns( collection.getKey() );
			if ( collection.isIndexed() ) {
				msg += ", index: " + columns( ( (IndexedCollection) collection ).getIndex() );
			}
			if ( collection.isOneToMany() ) {
				msg += ", one-to-many: "
					+ ( (OneToMany) collection.getElement() ).getReferencedEntityName();
			}
			else {
				msg += ", element: " + columns( collection.getElement() );
			}
			LOG.debug( msg );
		}
	}

	abstract public void secondPass(Map<String, PersistentClass> persistentClasses) throws MappingException;

	private static String columns(Value val) {
		StringBuilder columns = new StringBuilder();
		for ( Selectable selectable : val.getSelectables() ) {
			if ( columns.length() > 0 ) {
				columns.append( ", " );
			}
			columns.append( selectable.getText() );
		}
		return columns.toString();
	}
}
