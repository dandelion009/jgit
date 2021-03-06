/*
 * Copyright (C) 2009, Google Inc.
 * Copyright (C) 2008, Jonas Fonseca <fonseca@diku.dk>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.pgm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.FetchConnection;
import org.eclipse.jgit.transport.Transport;

@Command(common = true, usage = "usage_LsRemote")
class LsRemote extends TextBuiltin {
	@Option(name = "--timeout", metaVar = "metaVar_service", usage = "usage_abortConnectionIfNoActivity")
	int timeout = -1;

	@Argument(index = 0, metaVar = "metaVar_uriish", required = true)
	private String remote;

	@Argument(index = 1, metaVar = "metaVar_refs", handler = StringArrayOptionHandler.class)
	private String[] refs;

	@Override
	protected void run() throws Exception {
		final Transport tn = Transport.open(db, remote);
		final Patterns patterns = new Patterns(refs);
		if (0 <= timeout)
			tn.setTimeout(timeout);
		final FetchConnection c = tn.openFetch();
		try {
			for (final Ref r : c.getRefs()) {
				if (!patterns.match(r.getName()))
					continue;
				show(r.getObjectId(), r.getName());
				if (r.getPeeledObjectId() != null)
					show(r.getPeeledObjectId(), r.getName() + "^{}"); //$NON-NLS-1$
			}
		} finally {
			c.close();
			tn.close();
		}
	}

	@Override
	protected boolean requiresRepository() {
		return false;
	}

	private void show(final AnyObjectId id, final String name)
			throws IOException {
		outw.print(id.name());
		outw.print('\t');
		outw.print(name);
		outw.println();
	}

	// TODO: need implements https://github.com/git/git/blob/master/wildmatch.c
	static class TailMatcher {
		private Pattern tailPattern;

		public TailMatcher(String pattern) {
			String p = "(^|/)" //$NON-NLS-1$
					+ Pattern.quote(pattern).replaceAll("\\*", "\\\\E.*\\\\Q") //$NON-NLS-1$ //$NON-NLS-2$
					+ "$"; //$NON-NLS-1$
			tailPattern = Pattern.compile(p);
		}

		public boolean matche(String name) {
			return tailPattern.matcher(name).find();
		}
	}

	static class Patterns {
		private List<TailMatcher> patterns = new ArrayList<TailMatcher>();

		Patterns(String[] refs) {
			if (refs != null && refs.length > 0) {
				for (String pattern : refs) {
					if (pattern.length() != 0) {
						patterns.add(new TailMatcher(pattern));
					}
				}
			}
		}

		public boolean match(String name) {
			if (patterns.size() == 0)
				return true;
			for (TailMatcher pattern : patterns) {
				if (pattern.matche(name))
					return true;
			}
			return false;
		}

	}
}
