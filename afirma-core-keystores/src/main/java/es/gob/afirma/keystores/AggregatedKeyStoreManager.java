package es.gob.afirma.keystores;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.gob.afirma.core.misc.AOUtil;

/** Gestor de claves consistente a su vez en un agregado de varios gestores, que se tratan y manejan como
 * si fuese un gestor normal de un &uacute;nico almac&eacute;n.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s */
public class AggregatedKeyStoreManager extends AOKeyStoreManager {

	private final List<AOKeyStoreManager> ksms = new ArrayList<AOKeyStoreManager>();

	AggregatedKeyStoreManager(final AOKeyStoreManager mainKsm) {
		if (mainKsm == null) {
			throw new IllegalArgumentException("El gestor principal de almacenes no puede ser nulo"); //$NON-NLS-1$
		}
		addKeyStoreManager(mainKsm);
		setKeyStoreType(mainKsm.getType());
	}

	protected AggregatedKeyStoreManager() {
		// Vacio
	}

	/** Contruye un gestor de claves consistente a su vez en un agregado de varios gestores,
	 * a partir de un almac&acute;n principal.
	 * @param ksm Gestor de claves principal */
	public final void addKeyStoreManager(final AOKeyStoreManager ksm) {
		if (ksm != null) {
			// Si es preferente hay que eliminar antes de anadir los posibles duplicados que hubiese
			if (ksm.isPreferred()) {
				final String[] newAliases = ksm.getAliases();
				MessageDigest md = null;
				try {
					md = MessageDigest.getInstance("SHA1"); //$NON-NLS-1$
				}
				catch (final NoSuchAlgorithmException e) {
					LOGGER.severe(
						"No se ha podido instanciar el generador de huellas digitales SHA1, pueden aparecer duplicados en la lista de certificados: " + e //$NON-NLS-1$
					);
				}
				if (md != null) {
					for (final String alias : newAliases) {
						final String currentThumbprint;
						try {
							currentThumbprint = AOUtil.hexify(
								md.digest(ksm.getCertificate(alias).getEncoded()),
								false
							);
						}
						catch (final CertificateEncodingException e) {
							LOGGER.severe(
								"No se ha obtener la huela del certificado '" + alias + "', pueden aparecer duplicados en la lista de certificados: " + e //$NON-NLS-1$ //$NON-NLS-2$
							);
							continue;
						}
						LOGGER.info(
							"El certificado de huella '" + currentThumbprint + "' se tomara unicamente del almacen preferente" //$NON-NLS-1$ //$NON-NLS-2$
						);
						for (final AOKeyStoreManager currentKsm : this.ksms) {
							currentKsm.deactivateEntry(currentThumbprint);
						}
					}
				}
			}
			this.ksms.add(ksm);
		}
	}

	@Override
	public String[] getAliases() {
		final List<String> aliases = new ArrayList<String>();
		for (final AOKeyStoreManager ksm : this.ksms) {
			aliases.addAll(Arrays.asList(ksm.getAliases()));
		}
		return aliases.toArray(new String[0]);
	}

	@Override
	public X509Certificate getCertificate(final String alias) {
		for (final AOKeyStoreManager ksm : this.ksms) {
			if (Arrays.asList(ksm.getAliases()).contains(alias)) {
				return ksm.getCertificate(alias);
			}
		}
		LOGGER.warning(
			"El almacen no contiene ningun certificado con alias '" + alias + "', se devolvera null" //$NON-NLS-1$ //$NON-NLS-2$
		);
		return null;
	}

	@Override
	public KeyStore.PrivateKeyEntry getKeyEntry(final String alias) throws KeyStoreException,
                                                                           NoSuchAlgorithmException,
                                                                           UnrecoverableEntryException {
		for (final AOKeyStoreManager ksm : this.ksms) {
			if (Arrays.asList(ksm.getAliases()).contains(alias)) {
				return ksm.getKeyEntry(alias);
			}
		}
		LOGGER.warning(
			"El almacen no contiene ninguna clave con alias '" + alias + "', se devolvera null" //$NON-NLS-1$ //$NON-NLS-2$
		);
		return null;
	}

	@Override
	public X509Certificate[] getCertificateChain(final String alias) {
		for (final AOKeyStoreManager ksm : this.ksms) {
			if (Arrays.asList(ksm.getAliases()).contains(alias)) {
				return ksm.getCertificateChain(alias);
			}
		}
		LOGGER.warning(
			"El almacen no contiene ninguna cadena de certificados con alias '" + alias + "', se devolvera null" //$NON-NLS-1$ //$NON-NLS-2$
		);
		return null;
	}

	@Override
	public void refresh() throws IOException {
		for (final AOKeyStoreManager ksm : this.ksms) {
			ksm.refresh();
		}
	}

	@Override
	protected boolean lacksKeyStores() {
		return this.ksms.isEmpty();
	}

	@Override
	public boolean isKeyEntry(final String alias) throws KeyStoreException {
		for (final AOKeyStoreManager ksm : this.ksms) {
			if (Arrays.asList(ksm.getAliases()).contains(alias)) {
				return ksm.isKeyEntry(alias);
			}
		}
		throw new KeyStoreException(
			"Se ha pedido comprobar la clave privada de un certificado no contenido en este gestor" //$NON-NLS-1$
		);
	}

	/**
	 * Elimina todos los almacenes del de claves del almac&eacute;n agregado.
	 */
	public void removeAll() {
		this.ksms.clear();
	}
}
