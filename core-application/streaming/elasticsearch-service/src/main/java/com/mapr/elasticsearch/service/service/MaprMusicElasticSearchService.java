package com.mapr.elasticsearch.service.service;

import org.apache.hadoop.security.UserGroupInformation;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.ojai.Document;
import org.ojai.DocumentStream;
import org.ojai.store.Connection;
import org.ojai.store.DocumentStore;
import org.ojai.store.DriverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MaprMusicElasticSearchService {

    private static final String TEST_USER_NAME = "mapr";
    private static final String TEST_USER_GROUP = "mapr";
    private static final String CONNECTION_URL = "ojai:mapr:";

    private static final String MAPR_MUSIC_CHANGELOG = "/mapr_music_changelog";
    private static final String ARTISTS_CHANGELOG = MAPR_MUSIC_CHANGELOG + ":artists";
    private static final String ARTISTS_INDEX_NAME = "artists";
    private static final String ARTISTS_TYPE_NAME = "artist";

    private static final String ALBUMS_CHANGELOG = MAPR_MUSIC_CHANGELOG + ":albums";
    private static final String ALBUMS_INDEX_NAME = "albums";
    private static final String ALBUMS_TYPE_NAME = "album";

    private static final String[] INDEXED_FIELDS = new String[]{"name"};

    private static final String ALBUMS_TABLE_PATH = "/apps/albums";
    private static final String ARTISTS_TABLE_PATH = "/apps/artists";

    private static final String HOSTNAME = "localhost";
    private static final int PORT = 9300;

    private static final Logger log = LoggerFactory.getLogger(MaprMusicElasticSearchService.class);

    public void reinit() {

        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(HOSTNAME);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }

        // Create ES Client
        TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(inetAddress, PORT));


        // Delete indices
        client.admin().indices().delete(new DeleteIndexRequest(ALBUMS_INDEX_NAME)).actionGet();
        client.admin().indices().delete(new DeleteIndexRequest(ARTISTS_INDEX_NAME)).actionGet();

        // Recreate indices
        client.admin().indices().prepareCreate(ALBUMS_INDEX_NAME).get();
        client.admin().indices().prepareCreate(ARTISTS_INDEX_NAME).get();

        // Iterate over Album/Artist documents and send them to ElasticSearch
        indexJSONTableDocuments(client, ALBUMS_INDEX_NAME, ALBUMS_TYPE_NAME, ALBUMS_TABLE_PATH, INDEXED_FIELDS);
        indexJSONTableDocuments(client, ARTISTS_INDEX_NAME, ARTISTS_TYPE_NAME, ARTISTS_TABLE_PATH, INDEXED_FIELDS);

        client.close();
    }

    private void indexJSONTableDocuments(TransportClient client, String indexName, String typeName, String tablePath, String... fields) {

        loginTestUser(TEST_USER_NAME, TEST_USER_GROUP);

        // Create an OJAI connection to MapR cluster
        Connection connection = DriverManager.getConnection(CONNECTION_URL);

        // Get an instance of OJAI DocumentStore
        final DocumentStore store = connection.getStore(tablePath);

        DocumentStream documentStream = store.find(fields);
        for (Document document : documentStream) {

            IndexResponse response = client.prepareIndex(indexName, typeName, document.getId().getString())
                    .setSource(document.asJsonString(), XContentType.JSON)
                    .get();

            log.info("Elasticsearch Index Response: '{}'", response);
        }

        // Close this instance of OJAI DocumentStore
        store.close();

        // Close the OJAI connection and release any resources held by the connection
        connection.close();
    }

    private static void loginTestUser(String username, String group) {
        UserGroupInformation currentUgi = UserGroupInformation.createUserForTesting(username, new String[]{group});
        UserGroupInformation.setLoginUser(currentUgi);
    }

    public void start() {

        // Build and start service for the Artists table
        new MaprElasticSearchServiceBuilder()
                .withHostname(HOSTNAME)
                .withPort(PORT)
                .withIndexName(ARTISTS_INDEX_NAME)
                .withTypeName(ARTISTS_TYPE_NAME)
                .withChangelog(ARTISTS_CHANGELOG)
                .withFields(INDEXED_FIELDS) // only Artist's name will be sent to the ElasticSearch
                .build().start();

        // Build and start service for the Albums table
        new MaprElasticSearchServiceBuilder()
                .withHostname(HOSTNAME)
                .withPort(PORT)
                .withIndexName(ALBUMS_INDEX_NAME)
                .withTypeName(ALBUMS_TYPE_NAME)
                .withChangelog(ALBUMS_CHANGELOG)
                .withFields(INDEXED_FIELDS) // only Album's name will be sent to the ElasticSearch
                .build().start();
    }

}
