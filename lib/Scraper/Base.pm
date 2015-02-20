package Scraper::Base;
use Mojo::DOM;
use WWW::Mechanize;
use Carp;

sub new { bless {}, shift; }
sub mech {
    unless ( $_[0]->{mech} ){
        $_[0]->{mech} = WWW::Mechanize->new( cookie_jar => {} );
        $_[0]->{mech}->add_header( 'User-Agent' => 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36' );
    }
    return $_[0]->{mech};
}
sub get_dom {
    my $self = shift;

    my $mech = $self->mech();
    if ( my $url = shift ){
        $mech->get($url);
        unless ( $mech->success() ){
            confess 'Page fetch error: ' . $mech->res;
        }
    }

    my $dom = Mojo::DOM->new( $mech->content );
    return $dom;
}

1;
