package com.stockinteligence.estoque.infrastructure.adapter.in.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Testa só a fiação HTTP: request→Command/Query correto, status code e
 * formato de resposta. Um caso feliz por endpoint + um exemplo de cada
 * família de erro (400/404/409), total — não a matriz de regra de negócio
 * de spec.md, já coberta em ProdutoTest e nos *CommandHandlerTest (ver
 * memory/testing-strategy.md).
 */
@QuarkusTest
class ProdutoResourceTest {

    private static String skuUnico(String prefixo) {
        // SKU é normalizado para maiúsculas pelo VO (ver SKUTest) — gerado
        // já em maiúsculas aqui para as assertions comparem igual.
        return prefixo + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String corpoCadastro(String sku) {
        return """
                {
                  "sku": "%s",
                  "nome": "Refrigerante 2L",
                  "categoria": "BEBIDAS",
                  "unidadeMedida": "UN",
                  "precoCusto": 4.50,
                  "precoVenda": 7.90
                }
                """.formatted(sku);
    }

    private String cadastrarECapturarId(String sku) {
        return given()
                .contentType(ContentType.JSON)
                .body(corpoCadastro(sku))
                .when().post("/api/produtos")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("sku", equalTo(sku))
                .extract().path("id");
    }

    @Test
    void cadastrarProdutoRetorna201ComProdutoCriado() {
        cadastrarECapturarId(skuUnico("WEB-CAD"));
    }

    @Test
    void buscarPorIdRetorna200ComProdutoEncontrado() {
        String sku = skuUnico("WEB-BID");
        String id = cadastrarECapturarId(sku);

        given().when().get("/api/produtos/{id}", id)
                .then().statusCode(200)
                .body("sku", equalTo(sku));
    }

    @Test
    void buscarPorSkuRetorna200ComProdutoEncontrado() {
        String sku = skuUnico("WEB-BSK");
        cadastrarECapturarId(sku);

        given().when().get("/api/produtos/sku/{sku}", sku)
                .then().statusCode(200)
                .body("sku", equalTo(sku));
    }

    @Test
    void listarRetorna200ComConteudoPaginado() {
        cadastrarECapturarId(skuUnico("WEB-LST"));

        given().when().get("/api/produtos")
                .then().statusCode(200)
                .body("content", notNullValue())
                .body("page", notNullValue());
    }

    @Test
    void atualizarRetorna200ComDadosAlterados() {
        String sku = skuUnico("WEB-UPD");
        String id = cadastrarECapturarId(sku);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "nome": "Refrigerante 2L Cola",
                          "categoria": "BEBIDAS",
                          "unidadeMedida": "UN",
                          "precoCusto": 4.50,
                          "precoVenda": 8.50
                        }
                        """)
                .when().put("/api/produtos/{id}", id)
                .then().statusCode(200)
                .body("nome", equalTo("Refrigerante 2L Cola"))
                .body("sku", equalTo(sku));
    }

    @Test
    void inativarRetorna200ComStatusInativo() {
        String id = cadastrarECapturarId(skuUnico("WEB-INA"));

        given().when().patch("/api/produtos/{id}/inativar", id)
                .then().statusCode(200)
                .body("status", equalTo("INATIVO"));
    }

    @Test
    void reativarRetorna200ComStatusAtivo() {
        String id = cadastrarECapturarId(skuUnico("WEB-REA"));
        given().when().patch("/api/produtos/{id}/inativar", id).then().statusCode(200);

        given().when().patch("/api/produtos/{id}/reativar", id)
                .then().statusCode(200)
                .body("status", equalTo("ATIVO"));
    }

    @Test
    void cadastrarComDadosInvalidosRetorna400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "sku": "",
                          "nome": "",
                          "categoria": "BEBIDAS",
                          "unidadeMedida": "UN",
                          "precoCusto": 4.50,
                          "precoVenda": 7.90
                        }
                        """)
                .when().post("/api/produtos")
                .then().statusCode(400);
    }

    @Test
    void buscarProdutoInexistenteRetorna404() {
        given().when().get("/api/produtos/{id}", UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    void cadastrarComSkuDuplicadoRetorna409() {
        String sku = skuUnico("WEB-DUP");
        cadastrarECapturarId(sku);

        given()
                .contentType(ContentType.JSON)
                .body(corpoCadastro(sku))
                .when().post("/api/produtos")
                .then().statusCode(409);
    }
}
