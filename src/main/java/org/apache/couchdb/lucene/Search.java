package org.apache.couchdb.lucene;

import java.io.IOException;
import java.util.Scanner;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.NIOFSDirectory;

/**
 * Search entry point.
 */
public final class Search {

	public static void main(final String[] args) throws Exception {
		IndexReader reader = null;
		IndexSearcher searcher = null;

		final Scanner scanner = new Scanner(System.in);
		while (scanner.hasNextLine()) {
			if (reader == null) {
				// Open a reader and searcher if index exists.
				if (IndexReader.indexExists(Config.INDEX_DIR)) {
					reader = IndexReader.open(NIOFSDirectory.getDirectory(Config.INDEX_DIR), true);
					searcher = new IndexSearcher(reader);
				}
			} else {
				// Refresh reader and searcher if necessary.
				final IndexReader newReader = reader.reopen();
				if (reader != newReader) {
					Log.outlog("Lucene index was updated, reopening searcher.");
					final IndexReader oldReader = reader;
					reader = newReader;
					searcher = new IndexSearcher(reader);
					oldReader.close();
				}
			}

			final String line = scanner.nextLine();

			// Process search request if index exists.
			if (searcher == null) {
				System.out.println(Utils.error(503, "couchdb-lucene not available."));
				continue;
			}

			final JSONObject obj;
			try {
				obj = JSONObject.fromObject(line);
			} catch (final JSONException e) {
				System.out.println(Utils.error(400, "invalid JSON."));
				continue;
			}

			if (!obj.has("query")) {
				System.out.println(Utils.error(400, "No query found in request."));
				continue;
			}

			final JSONObject query = obj.getJSONObject("query");

			try {
				// A query.
				if (query.has("q")) {
					final SearchRequest request = new SearchRequest(obj);
					final String result = request.execute(searcher);
					System.out.println(result);
					continue;
				}
			} catch (final Exception e) {
				System.out.println(Utils.error(400, e.getMessage()));
			}

			System.out.println(Utils.error(400, "Bad request."));
		}
		if (reader != null) {
			reader.close();
		}
	}

}
