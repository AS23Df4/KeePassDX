/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database.edit;

import android.content.Context;

import com.keepassdroid.database.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwEntry;

public class AddEntry extends RunnableOnFinish {
	protected Database mDb;
	private PwEntry mEntry;
	private Context ctx;
	
	public AddEntry(Context ctx, Database db, PwEntry entry, OnFinish finish) {
		super(finish);
		
		mDb = db;
		mEntry = entry;
		this.ctx = ctx;
		
		mFinish = new AfterAdd(mFinish);
	}
	
	@Override
	public void run() {
		mDb.pm.addEntryTo(mEntry, mEntry.getParent());
		
		// Commit to disk
		SaveDB save = new SaveDB(ctx, mDb, mFinish);
		save.run();
	}
	
	private class AfterAdd extends OnFinish {

		AfterAdd(OnFinish finish) {
			super(finish);
		}

		@Override
		public void run() {
			PwDatabase pm = mDb.pm;
			if ( !mSuccess ) {
				pm.removeEntryFrom(mEntry, mEntry.getParent());
			}
			// TODO if add entry callback
			super.run();
		}
	}
}
