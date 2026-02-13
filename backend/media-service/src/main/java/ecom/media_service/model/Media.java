package ecom.media_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "media")
@CompoundIndex(name = "owner_type_owner_id_idx", def = "{'ownerType': 1, 'ownerId': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {

    @Id
    private String id;

    private String ownerId;
    private OwnerType ownerType;
    private String contentType;
    private String objectKey;
}
