package io.jans.orm.cloud.spanner.operation.impl.test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.WriteBuilder;

import io.jans.orm.cloud.spanner.operation.SpannerOperationService;
import io.jans.orm.cloud.spanner.operation.impl.SpannerConnectionProvider;

public class SpannerMultiValuedDataPopulatorTest {

	public static void main(String[] args) {
		Properties props = new Properties();
		props.setProperty("connection.project", "jans-server-184620");
		props.setProperty("connection.instance", "movchan");
		props.setProperty("connection.database", "test_interleave");
		props.setProperty("connection.client.create-max-wait-time-millis", "15");
		props.setProperty("connection.credentials-file", "V:\\Documents\\spanner\\full_access_spanner_sa.json");

		SpannerConnectionProvider connectionProvider = new SpannerConnectionProvider(props);
		connectionProvider.create();
		DatabaseClient client = connectionProvider.getClient();

		List<Mutation> mutations = new LinkedList<>();
		for (int i = 1; i <= 1000000; i++) {
			if (i % 50000 == 0) {
				System.out.println("Added: " + i);
			}

			String redirectURIs[] = new String[4];
			for (int j = 0; j < redirectURIs.length; j++) {
				redirectURIs[j] = String.valueOf(Math.round(Math.random() * 10));
			}

			if (i % 10000 == 0) {
				redirectURIs[3] = String.valueOf(Math.round(Math.random() * 2));
			}

			// Change to false to insert data into parent-child tables
			if (true) {
				WriteBuilder insertMutation = Mutation.newInsertOrUpdateBuilder("jansClnt_Array")
						.set(SpannerOperationService.DOC_ID).to(String.valueOf(i))
						.set(SpannerOperationService.OBJECT_CLASS).to("jansClnt").set("jansRedirectURI")
						.toStringArray(Arrays.asList(redirectURIs));

				mutations.add(insertMutation.build());
			} else {

				WriteBuilder insertMutation = Mutation.newInsertOrUpdateBuilder("jansClnt_Interleave")
						.set(SpannerOperationService.DOC_ID).to(String.valueOf(i))
						.set(SpannerOperationService.OBJECT_CLASS).to("jansClnt");

				mutations.add(insertMutation.build());

				for (int j = 0; j < redirectURIs.length; j++) {
					WriteBuilder insertDictMutation = Mutation
							.newInsertOrUpdateBuilder("jansClnt_Interleave_jansRedirectURI")
							.set(SpannerOperationService.DOC_ID).to(String.valueOf(i))
							.set(SpannerOperationService.DICT_DOC_ID).to(String.valueOf(j)).set("jansRedirectURI")
							.to(redirectURIs[j]);

					mutations.add(insertDictMutation.build());
				}
				client.write(mutations);
			}
			mutations.clear();
		}
		connectionProvider.destroy();
	}

}
