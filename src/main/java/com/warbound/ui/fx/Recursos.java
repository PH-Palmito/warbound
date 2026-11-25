package com.warbound.ui.fx;

import javafx.scene.image.Image;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Carregamento centralizado de imagens e fontes. As imagens ficam em cache:
 * a mesma ilustração aparece na Biblioteca, na mão e no campo, e decodificar
 * o PNG várias vezes seria desperdício.
 */
public final class Recursos {

    private static final Map<String, Image> CACHE = new HashMap<>();

    private Recursos() { }

    /** Retorna a imagem do caminho em resources, ou null se não existir ou estiver corrompida. */
    public static Image imagem(String caminho) {
        if (caminho == null) return null;
        if (CACHE.containsKey(caminho)) return CACHE.get(caminho);
        Image resultado = null;
        try {
            URL url = Recursos.class.getResource(caminho);
            if (url != null) {
                Image candidata = new Image(url.toExternalForm());
                if (!candidata.isError()) resultado = candidata;
            }
        } catch (Exception e) {
            resultado = null;
        }
        CACHE.put(caminho, resultado);
        return resultado;
    }

    /** Registra a fonte MedievalSharp. Deve ser chamado antes de criar a Scene. */
    public static void carregarFontes() {
        try (InputStream in = Recursos.class.getResourceAsStream("/fonts/MedievalSharp-Regular.ttf")) {
            if (in != null) {
                Font.loadFont(in, 14);
            }
        } catch (Exception e) {
            // sem a fonte o CSS cai para a fonte padrão do sistema
        }
    }
}
