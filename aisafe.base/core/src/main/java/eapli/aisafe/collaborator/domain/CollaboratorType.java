package eapli.aisafe.collaborator.domain;

/**
 * Identifies the role variant of a Collaborator.
 * Used as JPA discriminator instead of class inheritance.
 * ATC — employed by an AirTransportCompany (companyId required).
 * FCO — Flight Control Operator for an AirControlArea (areaCode required).
 * WEATHER — Weather Person for an AirControlArea (areaCode required).
 */
public enum CollaboratorType {
    ATC, FCO, WEATHER
}
