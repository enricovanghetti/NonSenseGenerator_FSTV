package com.example.syntaxtree;

import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController //controller REST
@RequestMapping("/tree") //base path
public class SyntaxTreeController {

    public static class Token {
        public String content; //testo
        public int headTokenIndex; //indice padre
        public String pos; //parte del discorso
    }

    @PostMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<ByteArrayResource> generateTreeImage(@RequestBody List<Token> tokens) throws Exception {
        mxGraph graph = new mxGraph(); //grafico base
        Object parent = graph.getDefaultParent();

        graph.getModel().beginUpdate();
        Object[] nodes = new Object[tokens.size()];
        try {
            for (int i = 0; i < tokens.size(); i++) {
                Token t = tokens.get(i);
                String label = t.content + "\n" + t.pos; //testo + tipo
                nodes[i] = graph.insertVertex(parent, null, label, 0, 0, 100, 40); //nodo
            }
            for (int i = 0; i < tokens.size(); i++) {
                int head = tokens.get(i).headTokenIndex;
                if (i != head && head >= 0 && head < tokens.size()) {
                    graph.insertEdge(parent, null, "", nodes[head], nodes[i]); //collegamento
                }
            }
        } finally {
            graph.getModel().endUpdate();
        }

        mxCompactTreeLayout layout = new mxCompactTreeLayout(graph); //layout ad albero
        layout.setHorizontal(false); //verticale
        layout.execute(parent); //applica layout

        BufferedImage image = mxCellRenderer.createBufferedImage(graph, null, 1, null, true, null); //crea immagine
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        ByteArrayResource resource = new ByteArrayResource(baos.toByteArray()); //buffer immagine

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tree.png") //nome file
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(resource.contentLength())
                .body(resource); //ritorna immagine come risposta
    }
}
