# When importing attributes, make sure to pick a name that does
# not consist only of the INUM, as this will not work with INUMs
# that start with a digit.
terraform import jans_attribute.attribute_BCE8 "BCE8"