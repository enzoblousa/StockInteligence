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
 * família de erro (400/404/409) — não a matriz de regra de negócio de
 * spec.md, já coberta em SaldoEstoqueTest e nos *CommandHandlerTest (ver
 * memory/testing-strategy.md).
 */
@QuarkusTest
class SaldoEstoqueResourceTest {

    private static String skuUnico(String prefixo) {
        return prefixo + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String cadastrarProdutoECapturarId() {
        String corpo = """
                {
                  "sku": "%s",
                  "nome": "Refrigerante 2L",
                  "categoria": "BEBIDAS",
                  "unidadeMedida": "UN",
                  "precoCusto": 4.50,
                  "precoVenda": 7.90
                }
                """.formatted(skuUnico("SLD-WEB"));

        return given()
                .contentType(ContentType.JSON)
                .body(corpo)
                .when().post("/api/produtos")
                .then().statusCode(201)
                .extract().path("id");
    }

    private void iniciarSaldo(String produtoId, String quantidadeInicial, String quantidadeMinima) {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "quantidadeInicial": %s, "quantidadeMinima": %s }
                        """.formatted(quantidadeInicial, quantidadeMinima))
                .when().post("/api/produtos/{produtoId}/saldo-estoque", produtoId)
                .then().statusCode(201);
    }

    @Test
    void iniciarSaldoRetorna201ComSaldoCriado() {
        String produtoId = cadastrarProdutoECapturarId();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "quantidadeInicial": 10, "quantidadeMinima": 5 }
                        """)
                .when().post("/api/produtos/{produtoId}/saldo-estoque", produtoId)
                .then().statusCode(201)
                .body("produtoId", equalTo(produtoId))
                .body("quantidadeAtual", notNullValue());
    }

    @Test
    void registrarEntradaRetorna200ComSaldoAtualizado() {
        String produtoId = cadastrarProdutoECapturarId();
        iniciarSaldo(produtoId, "10", "5");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "quantidade": 3 }
                        """)
                .when().post("/api/produtos/{produtoId}/saldo-estoque/entradas", produtoId)
                .then().statusCode(200)
                .body("quantidadeAtual", equalTo(13.0f));
    }

    @Test
    void registrarSaidaRetorna200ComSaldoAtualizado() {
        String produtoId = cadastrarProdutoECapturarId();
        iniciarSaldo(produtoId, "10", "5");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "quantidade": 3 }
                        """)
                .when().post("/api/produtos/{produtoId}/saldo-estoque/saidas", produtoId)
                .then().statusCode(200)
                .body("quantidadeAtual", equalTo(7.0f));
    }

    @Test
    void buscarPorProdutoIdRetorna200ComSaldoEncontrado() {
        String produtoId = cadastrarProdutoECapturarId();
        iniciarSaldo(produtoId, "10", "5");

        given().when().get("/api/produtos/{produtoId}/saldo-estoque", produtoId)
                .then().statusCode(200)
                .body("produtoId", equalTo(produtoId));
    }

    @Test
    void buscarPorSkuRetorna200ComSaldoEncontrado() {
        String produtoId = cadastrarProdutoECapturarId();
        iniciarSaldo(produtoId, "10", "5");
        String sku = given().when().get("/api/produtos/{produtoId}", produtoId).then().extract().path("sku");

        given().when().get("/api/saldo-estoque/sku/{sku}", sku)
                .then().statusCode(200)
                .body("sku", equalTo(sku));
    }

    @Test
    void iniciarSaldoComQuantidadeNegativaRetorna400() {
        String produtoId = cadastrarProdutoECapturarId();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "quantidadeInicial": -1, "quantidadeMinima": 5 }
                        """)
                .when().post("/api/produtos/{produtoId}/saldo-estoque", produtoId)
                .then().statusCode(400);
    }

    @Test
    void buscarSaldoInexistenteRetorna404() {
        given().when().get("/api/produtos/{produtoId}/saldo-estoque", UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    void iniciarSaldoDuasVezesRetorna409() {
        String produtoId = cadastrarProdutoECapturarId();
        iniciarSaldo(produtoId, "10", "5");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "quantidadeInicial": 20, "quantidadeMinima": 10 }
                        """)
                .when().post("/api/produtos/{produtoId}/saldo-estoque", produtoId)
                .then().statusCode(409);
    }
}
