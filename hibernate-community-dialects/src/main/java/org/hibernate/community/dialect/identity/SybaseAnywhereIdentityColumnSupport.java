/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect.identity;

import org.hibernate.dialect.identity.AbstractTransactSQLIdentityColumnSupport;

/**
 * @author Andrea Boriero
 */
public class SybaseAnywhereIdentityColumnSupport extends AbstractTransactSQLIdentityColumnSupport {

	public static final SybaseAnywhereIdentityColumnSupport INSTANCE = new SybaseAnywhereIdentityColumnSupport();

	@Override
	public boolean supportsInsertSelectIdentity() {
		return false;
	}
}
