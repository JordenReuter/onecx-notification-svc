package org.tkit.onecx.notification.domain.services;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.tkit.onecx.notification.domain.daos.NotificationDAO;
import org.tkit.onecx.notification.rs.external.v1.mappers.NotificationMapper;

import gen.org.tkit.onecx.notification.bff.client.api.NotificationInternalBffApi;
import gen.org.tkit.onecx.notification.rs.external.v1.model.NotificationDTOV1;
import io.quarkus.logging.Log;

@ApplicationScoped
public class NotificationService {
    @Inject
    NotificationDAO notificationDAO;

    @Inject
    NotificationMapper mapper;

    @Inject
    @RestClient
    NotificationInternalBffApi bffApi;

    @PostConstruct
    void init() {
        var notDeliveredNotifications = notificationDAO.findAllNotDelivered();
        notDeliveredNotifications.forEach(notification -> {
            try {
                bffApi.dispatchNotification(mapper.mapToBffDTO(notification));
            } catch (Exception ex) {
                Log.error("Error dispatching notification with id " + notification.getId(), ex);
            }
        });
    }

    public Response dispatchNotification(NotificationDTOV1 notificationDTOV1) {
        var notification = mapper.map(notificationDTOV1);
        if (notificationDTOV1.getPersist()) {
            notificationDAO.create(notification);
        }
        try (Response ignore = bffApi.dispatchNotification(mapper.mapToBffDTO(notification))) {
            return Response.status(Response.Status.OK).build();
        }
    }
}
