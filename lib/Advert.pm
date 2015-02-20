package Advert;
use strict; use warnings;
use Mo;

has url         => ( is => 'rw', isa => 'Str', required => 1 );
has title       => ( is => 'rw', isa => 'Str', required => 1 );

sub new {
    my $class = shift;
    if ( ref( $_[0] ) ){
        return bless $_[0], $class;
    }
    bless { @_ }, $class;
}

sub as_string { join( "\n", map { join( ": ", $_, $_[0]->{$_} ) } keys %{$_[0]} ) };

1;
