/*
 * Controlador Java de la Secretaría de Estado de Administraciones Públicas
 * para el DNI electrónico.
 *
 * El Controlador Java para el DNI electrónico es un proveedor de seguridad de JCA/JCE 
 * que permite el acceso y uso del DNI electrónico en aplicaciones Java de terceros 
 * para la realización de procesos de autenticación, firma electrónica y validación 
 * de firma. Para ello, se implementan las funcionalidades KeyStore y Signature para 
 * el acceso a los certificados y claves del DNI electrónico, así como la realización 
 * de operaciones criptográficas de firma con el DNI electrónico. El Controlador ha 
 * sido diseñado para su funcionamiento independiente del sistema operativo final.
 * 
 * Copyright (C) 2012 Dirección General de Modernización Administrativa, Procedimientos 
 * e Impulso de la Administración Electrónica
 * 
 * Este programa es software libre y utiliza un licenciamiento dual (LGPL 2.1+
 * o EUPL 1.1+), lo cual significa que los usuarios podrán elegir bajo cual de las
 * licencias desean utilizar el código fuente. Su elección deberá reflejarse 
 * en las aplicaciones que integren o distribuyan el Controlador, ya que determinará
 * su compatibilidad con otros componentes.
 *
 * El Controlador puede ser redistribuido y/o modificado bajo los términos de la 
 * Lesser GNU General Public License publicada por la Free Software Foundation, 
 * tanto en la versión 2.1 de la Licencia, o en una versión posterior.
 * 
 * El Controlador puede ser redistribuido y/o modificado bajo los términos de la 
 * European Union Public License publicada por la Comisión Europea, 
 * tanto en la versión 1.1 de la Licencia, o en una versión posterior.
 * 
 * Debería recibir una copia de la GNU Lesser General Public License, si aplica, junto
 * con este programa. Si no, consúltelo en <http://www.gnu.org/licenses/>.
 * 
 * Debería recibir una copia de la European Union Public License, si aplica, junto
 * con este programa. Si no, consúltelo en <http://joinup.ec.europa.eu/software/page/eupl>.
 *
 * Este programa es distribuido con la esperanza de que sea útil, pero
 * SIN NINGUNA GARANTÍA; incluso sin la garantía implícita de comercialización
 * o idoneidad para un propósito particular.
 */
package es.gob.jmulticard.apdu.connection.cwa14890;

import es.gob.jmulticard.apdu.CommandApdu;

/** APDU cifrada para su env&iacute;o a trav&eacute;s de un canal seguro.
 * @author Carlos Gamuci
 * @author Alberto Mart&iacute;nez */
final class CipheredApdu extends CommandApdu {

	private static final byte TAG_CRYPTOGRAPHIC_CHECKSUM = (byte) 0x8E;

	private final byte[] mac;
	private final byte[] data;

    byte[] getMac() {
        final byte[] out = new byte[this.mac.length];
        System.arraycopy(this.mac, 0, out, 0, this.mac.length);
        return out;
    }

    byte[] getCryptogramData() {
        final byte[] out = new byte[this.data.length];
        System.arraycopy(this.data, 0, out, 0, this.data.length);
        return out;
    }

    /** Crea una APDU cifrada seg&uacute;n CWA-14890.
     * @param data Datos del TLV criptograma */
    CipheredApdu(final byte cla, final byte ins, final byte p1, final byte p2, final byte[] data, final byte[] mac) {
        super(
    		cla,					// CLA
    		ins,					// INS
    		p1,						// P1
    		p2,						// P2
    		buildData(data, mac),	// Data
    		null					// Le
		);
        this.mac = new byte[mac.length];
        System.arraycopy(mac, 0, this.mac, 0, mac.length);
        this.data = new byte[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    private static byte[] buildData(final byte[] data, final byte[] mac) {
       if (data == null || mac == null) {
        	throw new IllegalArgumentException("Ni los datos (TLV) ni el MAC pueden ser nulos"); //$NON-NLS-1$
       }
       if (mac.length != 4) {
        	throw new IllegalArgumentException("El MAC debe medir exactamente cuatro octetos"); //$NON-NLS-1$
       }

       final int retLength = data.length + mac.length + 2;

       // Agregamos el TLV de Datos
       final byte[] ret = new byte[retLength];
       if (data.length > 0) {
    	   System.arraycopy(data, 0, ret, 0, data.length);
       }

       // Agregamos el TLV del CheckSum
       ret[ret.length-1] = mac[3];
       ret[ret.length-2] = mac[2];
       ret[ret.length-3] = mac[1];
       ret[ret.length-4] = mac[0];
       ret[ret.length-5] = (byte) 0x04; // Long del Mac
       ret[ret.length-6] = TAG_CRYPTOGRAPHIC_CHECKSUM;

       return ret;
    }

    /** {@inheritDoc} */
    public void setLe(final int le) {
        throw new UnsupportedOperationException("No se puede establecer el Le en una APDU cifrada"); //$NON-NLS-1$
    }

}