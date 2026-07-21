package com.crafthub.delivery_service.converter;

import com.crafthub.delivery_service.entity.DeliveryProvider;
import com.crafthub.delivery_service.entity.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LogisticsStatusMapperTest {

    private LogisticsStatusMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LogisticsStatusMapper();
    }

    @ParameterizedTest
    @CsvSource({
            "'Очікується відправлення', PREPARING",
            "'Створено електронну заявку', PREPARING",
            "'Відправлення прийнято', SHIPPED",
            "'Прямує до міста', SHIPPED",
            "'В дорозі', SHIPPED",
            "'Прибув у відділення', SHIPPED",
            "'Відправлення отримано', DELIVERED",
            "'Доставлено', DELIVERED",
            "'Одержано', DELIVERED",
            "'Відмова від отримання', RETURNED",
            "'Відправлення повертається', RETURNED",
            "'Створена заявка видалено', CANCELLED"
    })
    void testNovaPoshtaMappings(String externalStatus, DeliveryStatus expectedStatus) {
        DeliveryStatus status = mapper.mapStatus(DeliveryProvider.NOVA_POSHTA, externalStatus);
        assertThat(status).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "'Відправлення прийняте', PREPARING",
            "'Знаходиться в дорозі', SHIPPED",
            "'Відправлено', SHIPPED",
            "'Вручено', DELIVERED",
            "'Доставлено', DELIVERED",
            "'Відмова', RETURNED"
    })
    void testUkrposhtaMappings(String externalStatus, DeliveryStatus expectedStatus) {
        DeliveryStatus status = mapper.mapStatus(DeliveryProvider.UKRPOSHTA, externalStatus);
        assertThat(status).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "'Готово до відправки', READY_TO_SHIP",
            "'В дорозі', SHIPPED",
            "'Доставлено', DELIVERED"
    })
    void testSellerMappings(String externalStatus, DeliveryStatus expectedStatus) {
        DeliveryStatus status = mapper.mapStatus(DeliveryProvider.SELLER, externalStatus);
        assertThat(status).isEqualTo(expectedStatus);
    }

    @Test
    void testUnknownStatusReturnsNull() {
        DeliveryStatus status = mapper.mapStatus(DeliveryProvider.NOVA_POSHTA, "Some random status");
        assertThat(status).isNull();
    }

    @Test
    void testNullOrBlankStatusReturnsNull() {
        assertThat(mapper.mapStatus(DeliveryProvider.NOVA_POSHTA, null)).isNull();
        assertThat(mapper.mapStatus(DeliveryProvider.NOVA_POSHTA, "")).isNull();
        assertThat(mapper.mapStatus(DeliveryProvider.NOVA_POSHTA, "   ")).isNull();
    }
}
