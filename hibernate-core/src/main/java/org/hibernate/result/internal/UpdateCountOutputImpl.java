/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.result.internal;

import org.hibernate.result.UpdateCountOutput;

/**
 * Implementation of UpdateCountOutput
 *
 * @author Steve Ebersole
 */
class UpdateCountOutputImpl implements UpdateCountOutput {
	private final int updateCount;

	public UpdateCountOutputImpl(int updateCount) {
		this.updateCount = updateCount;
	}

	@Override
	public int getUpdateCount() {
		return updateCount;
	}

	@Override
	public boolean isResultSet() {
		return false;
	}
}
