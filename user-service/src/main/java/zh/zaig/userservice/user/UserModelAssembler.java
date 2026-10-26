package zh.zaig.userservice.user;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import zh.zaig.userservice.user.dto.UserResponse;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class UserModelAssembler implements RepresentationModelAssembler<UserResponse, EntityModel<UserResponse>> {

    @Override
    public EntityModel<UserResponse> toModel(UserResponse user) {
        return EntityModel.of(user,
                linkTo(methodOn(UserController.class).getById(user.id())).withSelfRel(),
                linkTo(methodOn(UserController.class).getAll()).withRel("users"));
    }

    @Override
    public CollectionModel<EntityModel<UserResponse>> toCollectionModel(Iterable<? extends UserResponse> users) {
        CollectionModel<EntityModel<UserResponse>> model = RepresentationModelAssembler.super.toCollectionModel(users);
        model.add(linkTo(methodOn(UserController.class).getAll()).withSelfRel());
        return model;
    }
}
