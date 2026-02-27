package br.com.fightConnect.domain.contracts.services;

import br.com.fightConnect.domain.contracts.storage.StoredFile;

public interface FileStorageService {
	/**
	 * Salva o arquivo no storage e retorna: - storageKey: caminho/chave interna do
	 * arquivo no disco - url: url pública (ex: /images/empresa/uuid.jpg)
	 */
	StoredFile save(byte[] bytes, String contentType, String fileName, String folder);

	void delete(String storageKey);
}
