/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.kunzisoft.keepass.database.search;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.element.PwDatabase;
import com.kunzisoft.keepass.database.element.PwDatabaseV3;
import com.kunzisoft.keepass.database.element.PwDatabaseV4;
import com.kunzisoft.keepass.database.element.PwEntryInterface;
import com.kunzisoft.keepass.database.element.PwGroupInterface;
import com.kunzisoft.keepass.database.iterator.EntrySearchStringIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class SearchDbHelper<PwDatabaseVersion extends PwDatabase> {
	
	private final Context mCtx;
	
	public SearchDbHelper(Context ctx) {
		this.mCtx = ctx;
	}
	
	private boolean omitBackup() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mCtx);
		return prefs.getBoolean(mCtx.getString(R.string.omitbackup_key), mCtx.getResources().getBoolean(R.bool.omitbackup_default));
	}

	public PwGroupInterface search(PwDatabaseVersion pm, String qStr, int max) {

		PwGroupInterface group = pm.createGroup();
		group.setName("\"" + qStr + "\"");
		group.setEntries(new ArrayList<>());
		
		// Search all entries
		Locale loc = Locale.getDefault();
		qStr = qStr.toLowerCase(loc);
		boolean isOmitBackup = omitBackup();

		// TODO Search from the current group
		Queue<PwGroupInterface> worklist = new LinkedList<>();
		if (pm.getRootGroup() != null) {
			worklist.add(pm.getRootGroup());
		}

		while (worklist.size() != 0) {
			PwGroupInterface top = worklist.remove();
			
			if (pm.isGroupSearchable(top, isOmitBackup)) {
				for (PwEntryInterface entry : top.getChildEntries()) {
					processEntries(entry, group.getChildEntries(), qStr, loc);
					if (group.numbersOfChildEntries() >= max)
					    return group;
				}
				
				for (PwGroupInterface childGroup : top.getChildGroups()) {
					if (childGroup != null) {
						worklist.add(childGroup);
					}
				}
			}
		}
		
		return group;
	}
	
	private void processEntries(PwEntryInterface entry, List<PwEntryInterface> results, String qStr, Locale loc) {
		// Search all strings in the entry
		Iterator<String> iter = EntrySearchStringIterator.getInstance(entry);
		while (iter.hasNext()) {
			String str = iter.next();
			if (str != null && str.length() != 0) {
				String lower = str.toLowerCase(loc);
				if (lower.contains(qStr)) {
					results.add(entry);
					break;
				}
			}
		}
	}

	public static class SearchDbHelperV3 extends SearchDbHelper<PwDatabaseV3>{

		public SearchDbHelperV3(Context ctx) {
			super(ctx);
		}
	}

	public static class SearchDbHelperV4 extends SearchDbHelper<PwDatabaseV4>{

		public SearchDbHelperV4(Context ctx) {
			super(ctx);
		}
	}
	
}
